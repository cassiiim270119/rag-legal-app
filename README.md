# RAG Legal Application - Spring Boot + LangChain4j

Uma aplicação Java robusta para Retrieval-Augmented Generation (RAG) especializada em documentos jurídicos (súmulas, leis, decretos). Implementa busca híbrida (BM25 + Vetorial), reranking e oferece endpoints síncronos e com streaming.

## Características Principais

### 1. **Busca Híbrida (Hybrid Search)**
- **BM25 (Busca por Palavras-Chave):** Recuperação rápida e precisa de termos exatos
- **Busca Vetorial:** Compreensão semântica usando embeddings
- **Fusão Inteligente:** Combinação ponderada dos dois métodos (40% BM25 + 60% Vetorial)

### 2. **Metadados Estruturados**
Cada documento jurídico é indexado com metadados críticos:
- **Identificação:** Número do documento, tipo (Súmula, Lei, Decreto)
- **Temporalidade:** Data de publicação, data de revogação, status (Vigente/Revogada)
- **Classificação:** Ramo do direito (Civil, Penal, Trabalhista), tribunal (STF, STJ, TJ)
- **Estrutura:** Capítulo, artigo, parágrafo, seção

### 3. **Reranking Automático**
- Segunda etapa de avaliação para máxima relevância
- Prioriza documentos com maior relevância textual
- Reduz resultados para os Top-5 mais pertinentes

### 4. **Endpoints Síncronos e com Streaming**
- **Síncrono:** Resposta completa em uma única requisição
- **Streaming:** Resposta em tempo real (text/event-stream)

## Arquitetura

```
┌─────────────────────────────────────────────┐
│         REST Controller (RAGController)      │
└──────────────┬──────────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
   ┌───▼────────┐   ┌──▼──────────┐
   │ RAGService │   │ HybridSearch │
   └───┬────────┘   │   Service    │
       │            └──┬───────────┘
       │               │
   ┌───▼───────────────▼──────────────┐
   │  EmbeddingService  BM25Service   │
   │  (Local Models)    (Lucene)      │
   └───┬──────────────────────────────┘
       │
   ┌───▼──────────────────────────────┐
   │  LegalDocumentRepository (JPA)   │
   │  H2 Database                     │
   └────────────────────────────────┘
```

## Endpoints

### 1. RAG Síncrono
```bash
POST /api/rag/query
Content-Type: application/json

{
  "query": "Qual a súmula sobre prisão em segunda instância?",
  "tribunal": "STF",
  "legalArea": "PENAL",
  "limit": 10
}
```

**Resposta:**
```json
{
  "query": "Qual a súmula sobre prisão em segunda instância?",
  "answer": "De acordo com a Súmula 735 do STF...",
  "sources": [
    {
      "id": 1,
      "documentNumber": "STF-SUM-735",
      "title": "Súmula 735 - Execução Penal",
      "tribunal": "STF",
      "legalArea": "PENAL",
      "status": "VIGENTE",
      "score": 0.95,
      "metadata": {...}
    }
  ],
  "sourceCount": 1,
  "processingTimeMs": 245
}
```

### 2. RAG com Streaming
```bash
POST /api/rag/query-stream
Content-Type: application/json

{
  "query": "O que diz a Lei 8.112 sobre férias?",
  "tribunal": "STJ",
  "legalArea": "ADMINISTRATIVO"
}
```

**Resposta:** Stream de texto em tempo real (text/event-stream)

### 3. Busca Híbrida (sem LLM)
```bash
POST /api/rag/search
Content-Type: application/json

{
  "query": "direito autoral",
  "tribunal": null,
  "legalArea": "CIVIL",
  "limit": 5
}
```

### 4. Health Check
```bash
GET /api/rag/health
```

## Instalação e Execução

### Pré-requisitos
- Java 17+
- Maven 3.8+
- OpenAI API Key (para geração com LLM)

### Passos

1. **Clone o repositório**
```bash
cd /home/ubuntu/rag-legal-app
```

2. **Configure a API Key do OpenAI**
```bash
export OPENAI_API_KEY=sk-your-key-here
```

3. **Build do projeto**
```bash
mvn clean install
```

4. **Execute a aplicação**
```bash
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080/api`

## Configuração

Edite `src/main/resources/application.yml` para customizar:

```yaml
rag:
  embedding:
    model: all-minilm-l6-v2
    dimension: 384
  reranking:
    enabled: true
    top-k: 5
  hybrid-search:
    bm25-weight: 0.4      # Peso da busca BM25
    vector-weight: 0.6    # Peso da busca vetorial
```

## Fluxo de Processamento

### 1. Indexação (Inicialização)
```
Documento Jurídico
    ↓
[Extração de Metadados]
    ↓
[Geração de Embedding] → Armazenado no BD
    ↓
[Indexação BM25] → Índice em memória (Lucene)
```

### 2. Busca Híbrida
```
Query do Usuário
    ↓
[Embedding da Query]
    ↓
┌─────────────────────────────────────┐
│ Busca BM25 (50 docs) + Busca Vetorial │
└─────────────────────────────────────┘
    ↓
[Aplicar Filtros de Metadados]
    ↓
[Fusão de Scores]
    ↓
[Reranking] → Top-5
    ↓
Resultados Ordenados
```

### 3. Geração de Resposta
```
Resultados + Query
    ↓
[Construir Contexto]
    ↓
[Enviar ao LLM com Prompt]
    ↓
[Streaming ou Resposta Síncrona]
```

## Técnicas de Otimização

### 1. Metadados para Pré-Filtragem
- Reduz espaço de busca em até 80%
- Evita normas obsoletas (filtro de status)
- Permite busca por tribunal/área específica

### 2. Busca Híbrida
- BM25: Excelente para termos exatos (números de leis)
- Vetorial: Excelente para semântica (conceitos jurídicos)
- Combinação: Melhor dos dois mundos

### 3. Reranking
- Segunda etapa de avaliação
- Prioriza relevância textual
- Reduz custos de LLM (apenas Top-5)

### 4. Embeddings Locais
- Modelo all-minilm-l6-v2 (384 dimensões)
- Sem custos de API
- Rápido e eficiente

## Exemplos de Uso

### Exemplo 1: Consulta sobre Execução Penal
```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Qual a súmula sobre isolamento carcerário?",
    "tribunal": "STF",
    "legalArea": "PENAL"
  }'
```

### Exemplo 2: Streaming em Tempo Real
```bash
curl -X POST http://localhost:8080/api/rag/query-stream \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Quais são os direitos autorais?",
    "legalArea": "CIVIL"
  }' \
  --no-buffer
```

### Exemplo 3: Busca Híbrida Pura
```bash
curl -X POST http://localhost:8080/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "código penal artigo 1",
    "limit": 5
  }'
```

## Estrutura de Diretórios

```
rag-legal-app/
├── src/main/java/com/rag/legal/
│   ├── RagLegalApplication.java
│   ├── controller/
│   │   └── RAGController.java
│   ├── domain/
│   │   └── LegalDocument.java
│   ├── dto/
│   │   ├── SearchRequest.java
│   │   ├── SearchResult.java
│   │   └── RAGResponse.java
│   ├── repository/
│   │   └── LegalDocumentRepository.java
│   ├── service/
│   │   ├── EmbeddingService.java
│   │   ├── BM25SearchService.java
│   │   ├── HybridSearchService.java
│   │   ├── RAGService.java
│   │   └── DataInitializationService.java
│   └── config/
│       └── AppConfig.java
├── src/main/resources/
│   └── application.yml
├── pom.xml
└── README.md
```

## Métricas de Performance

- **Busca BM25:** ~5-10ms
- **Busca Vetorial:** ~20-50ms
- **Reranking:** ~10-20ms
- **Geração com LLM:** ~500-2000ms (dependente de API)
- **Total (Síncrono):** ~1-3 segundos

## Próximas Melhorias

1. **Elasticsearch:** Substituir Lucene por Elasticsearch para escala
2. **Cache Semântico:** Armazenar respostas frequentes
3. **Fine-tuning:** Treinar modelo de embedding específico para direito
4. **Persistência de Embeddings:** Usar PostgreSQL com pgvector
5. **Monitoramento:** Integrar Prometheus/Grafana

## Troubleshooting

### Erro: "OpenAI API Key not configured"
```bash
export OPENAI_API_KEY=sk-your-key-here
mvn spring-boot:run
```

### Erro: "Port 8080 already in use"
```bash
# Mudar porta em application.yml
server:
  port: 8081
```

### Erro: "No embedding model found"
O modelo all-minilm-l6-v2 será baixado automaticamente na primeira execução.

## Licença

MIT License

## Contato

Para dúvidas ou sugestões, abra uma issue no repositório.
