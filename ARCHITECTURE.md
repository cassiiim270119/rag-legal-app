# Arquitetura - RAG Legal Application

## Visão Geral

A aplicação RAG Legal é um sistema de Retrieval-Augmented Generation (RAG) especializado em documentos jurídicos, construído com Spring Boot e LangChain4j. Implementa busca híbrida, reranking e oferece endpoints síncronos e com streaming.

## Decisões Arquiteturais

### 1. Stack Tecnológico

| Componente | Tecnologia | Justificativa |
|-----------|-----------|---------------|
| Framework Web | Spring Boot 3.2 | Produção, ecossistema robusto, fácil integração |
| Linguagem | Java 17 | Avançado em Java, performance, tipagem forte |
| LLM Framework | LangChain4j 0.31.0 | Padrão de fato em Java, API idiomática |
| Busca BM25 | Apache Lucene 9.8 | Implementação em memória, rápida, sem dependências |
| Embeddings | all-minilm-l6-v2 | Local, sem custos, 384 dimensões |
| Database | H2 (dev), PostgreSQL (prod) | Desenvolvimento rápido, escalável em produção |
| API REST | Spring Web MVC + WebFlux | Síncrono e reativo, streaming nativo |

### 2. Busca Híbrida (Hybrid Search)

```
Query do Usuário
    ↓
┌─────────────────────────────────────────┐
│  Busca BM25 (Lucene)                    │
│  - Recupera 50 documentos               │
│  - Busca por palavras-chave exatas      │
│  - Score normalizado [0, 1]             │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│  Busca Vetorial (Embeddings)            │
│  - Embedding da query                   │
│  - Similaridade de cosseno              │
│  - Score normalizado [0, 1]             │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│  Fusão de Scores (Weighted Sum)         │
│  Score Híbrido = 0.4 * BM25 + 0.6 * Vec │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│  Filtros de Metadados                   │
│  - Tribunal (STF, STJ, TJ, etc)         │
│  - Área Legal (Civil, Penal, etc)       │
│  - Status (Vigente, Revogada)           │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│  Reranking (Top-5)                      │
│  - Prioriza relevância textual          │
│  - Reduz custos de LLM                  │
└─────────────────────────────────────────┘
    ↓
Resultados Ordenados
```

### 3. Metadados Estruturados

Cada documento jurídico é indexado com metadados críticos:

```java
LegalDocument {
    // Identificação
    documentNumber: "STF-SUM-735"
    documentType: "SUMULA"
    title: "Súmula 735 - Execução Penal"
    
    // Temporalidade
    publicationDate: 2003-11-13
    revocationDate: null
    status: "VIGENTE"
    
    // Classificação
    legalArea: "PENAL"
    tribunal: "STF"
    
    // Estrutura
    chapter: "EXECUÇÃO PENAL"
    article: "1"
    paragraph: null
    
    // Embeddings
    embedding: [0.123, -0.456, ..., 0.789] // 384 dimensões
}
```

### 4. Camadas da Aplicação

```
┌─────────────────────────────────────────┐
│         REST Controller Layer           │
│  (RAGController)                        │
│  - /api/rag/query (Síncrono)           │
│  - /api/rag/query-stream (Streaming)   │
│  - /api/rag/search (Busca Pura)        │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│      Service Layer                      │
│  ┌──────────────────────────────────┐   │
│  │ RAGService                       │   │
│  │ - Orquestra busca + LLM          │   │
│  │ - Síncrono e streaming           │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │ HybridSearchService              │   │
│  │ - Busca híbrida                  │   │
│  │ - Reranking                      │   │
│  │ - Filtros de metadados           │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │ EmbeddingService                 │   │
│  │ - Gera embeddings locais         │   │
│  │ - Similaridade de cosseno        │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │ BM25SearchService                │   │
│  │ - Indexação Lucene               │   │
│  │ - Busca por palavras-chave       │   │
│  └──────────────────────────────────┘   │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│      Repository Layer (JPA)             │
│  LegalDocumentRepository                │
│  - Queries customizadas                 │
│  - Filtros por tribunal, área, status   │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│      Database Layer                     │
│  H2 (Desenvolvimento)                   │
│  PostgreSQL (Produção)                  │
└─────────────────────────────────────────┘
```

## Fluxos de Dados

### Fluxo 1: Indexação (Inicialização)

```
DataInitializationService.run()
    ↓
createSampleDocuments()
    ↓
documentRepository.saveAll()
    ↓
Para cada documento:
    ├─ EmbeddingService.embed() → float[]
    ├─ Salvar embedding no BD
    └─ BM25SearchService.indexDocument()
    ↓
Pronto para buscas
```

### Fluxo 2: RAG Síncrono

```
POST /api/rag/query
    ↓
RAGController.querySync()
    ↓
RAGService.ragSync()
    ├─ HybridSearchService.hybridSearch()
    │  ├─ BM25SearchService.search() → 50 docs
    │  ├─ EmbeddingService.embed(query)
    │  ├─ Calcular similaridade vetorial
    │  ├─ Aplicar filtros de metadados
    │  ├─ Fusão de scores
    │  └─ Reranking → Top-5
    │
    ├─ buildContext(sources)
    │
    └─ generateAnswer(query, context)
       ├─ OpenAiChatModel.chat()
       └─ Retornar resposta
    ↓
RAGResponse (JSON)
```

### Fluxo 3: RAG com Streaming

```
POST /api/rag/query-stream
    ↓
RAGController.queryStream()
    ↓
RAGService.ragStream()
    ├─ HybridSearchService.hybridSearch()
    │  └─ [mesmo que Fluxo 2]
    │
    ├─ buildContext(sources)
    │
    └─ generateAnswerStream(query, context)
       ├─ OpenAiStreamingChatModel.chat()
       └─ Emitir tokens via Flux<String>
    ↓
text/event-stream (Streaming)
```

## Otimizações Implementadas

### 1. Pré-Filtragem com Metadados
- **Impacto:** Reduz espaço de busca em até 80%
- **Implementação:** Filtros no `HybridSearchService` antes da fusão de scores
- **Benefício:** Menor custo computacional, respostas mais rápidas

### 2. Busca Híbrida
- **Impacto:** Combina precisão (BM25) com semântica (Vetorial)
- **Implementação:** Weighted sum (40% BM25 + 60% Vetorial)
- **Benefício:** Melhor recall e precision

### 3. Reranking
- **Impacto:** Reduz resultados para Top-5 mais relevantes
- **Implementação:** Heurística de contagem de matches textuais
- **Benefício:** Menor contexto para LLM, custo reduzido

### 4. Embeddings Locais
- **Impacto:** Sem custos de API, latência baixa
- **Implementação:** all-minilm-l6-v2 (384 dimensões)
- **Benefício:** Privacidade, performance, escalabilidade

### 5. Índice BM25 em Memória
- **Impacto:** Busca rápida por palavras-chave
- **Implementação:** Apache Lucene com ByteBuffersDirectory
- **Benefício:** Sem I/O de disco, latência < 10ms

## Padrões de Design

### 1. Service Layer Pattern
- Lógica de negócio centralizada
- Fácil de testar e manter
- Reutilizável entre controllers

### 2. Repository Pattern
- Abstração do banco de dados
- Queries customizadas e type-safe
- Facilita migração de BD

### 3. DTO Pattern
- Separação entre camadas
- Validação de entrada
- Serialização JSON automática

### 4. Builder Pattern
- Construção fluente de objetos
- Legibilidade melhorada
- Implementado via Lombok

### 5. Streaming Pattern
- Resposta em tempo real
- Melhor UX para respostas longas
- Implementado com Reactor Flux

## Escalabilidade

### Vertical (Single Machine)
- Aumentar RAM para mais embeddings em cache
- Aumentar CPU para processamento paralelo
- Índice Lucene em memória

### Horizontal (Multiple Machines)
- Substituir Lucene por Elasticsearch
- Usar PostgreSQL com pgvector
- Load balancer (Nginx, HAProxy)
- Cache distribuído (Redis)

### Arquitetura Escalável Futura

```
┌─────────────────────────────────────────┐
│         Load Balancer (Nginx)           │
└────────────┬────────────────────────────┘
             │
    ┌────────┴────────┬────────┐
    ↓                 ↓        ↓
┌─────────┐      ┌─────────┐ ┌─────────┐
│ RAG App │      │ RAG App │ │ RAG App │
│ Instance│      │Instance │ │Instance │
└────┬────┘      └────┬────┘ └────┬────┘
     │                │           │
     └────────────────┼───────────┘
                      ↓
          ┌──────────────────────┐
          │   PostgreSQL + pgvector
          │   (Shared Database)  │
          └──────────────────────┘
                      ↑
          ┌──────────────────────┐
          │   Elasticsearch      │
          │   (Índice BM25)      │
          └──────────────────────┘
                      ↑
          ┌──────────────────────┐
          │   Redis Cache        │
          │   (Semantic Cache)   │
          └──────────────────────┘
```

## Segurança

### Implementado
- Validação de entrada (SearchRequest)
- Tratamento de exceções
- Logging estruturado

### Recomendado para Produção
- Autenticação (OAuth2, JWT)
- Rate limiting
- CORS configurado
- HTTPS/TLS
- Sanitização de queries
- Auditoria de acesso

## Monitoramento

### Métricas Recomendadas
- Latência de busca (p50, p95, p99)
- Latência de LLM
- Taxa de erro
- Uso de memória
- Tamanho do índice
- Quantidade de documentos

### Logging
- Nível DEBUG em desenvolvimento
- Nível INFO em produção
- Structured logging (JSON)
- Correlação de requisições

## Próximas Melhorias

### Curto Prazo
1. Implementar cache semântico (Redis)
2. Adicionar autenticação (OAuth2)
3. Integrar Elasticsearch
4. Fine-tuning de embeddings

### Médio Prazo
1. Migrar para PostgreSQL + pgvector
2. Implementar reranking com LLM
3. Adicionar suporte a múltiplas línguas
4. Dashboard de monitoramento

### Longo Prazo
1. Treinar modelo de embedding específico para direito
2. Implementar agent com múltiplas ferramentas
3. Suporte a documentos em PDF/Word
4. Integração com sistemas jurídicos reais

## Referências

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Lucene Documentation](https://lucene.apache.org/)
- [RAG Best Practices](https://www.anthropic.com/research/building-effective-agents)

---

Última atualização: 2026-04-04
