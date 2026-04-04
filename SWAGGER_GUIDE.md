# Guia de Uso - Swagger/OpenAPI

## 📖 Visão Geral

A aplicação RAG Legal agora possui documentação interativa completa via Swagger UI. Você pode testar todos os endpoints diretamente pela interface web.

## 🌐 Acessar Swagger

### Local (Desenvolvimento)
```
http://localhost:8080/api/swagger-ui.html
```

### Produção (Render)
```
https://rag-legal-app.onrender.com/api/swagger-ui.html
```

## 📊 JSON OpenAPI

Para obter a especificação OpenAPI em JSON:

### Local
```
http://localhost:8080/api/v3/api-docs
```

### Produção
```
https://rag-legal-app.onrender.com/api/v3/api-docs
```

## 🎯 Funcionalidades do Swagger

### 1. Visualizar Todos os Endpoints
A interface mostra todos os endpoints organizados por tags:
- **RAG Endpoints** - Busca e geração de respostas
- **PDF Upload** - Upload e indexação de PDFs

### 2. Testar Endpoints Interativamente
Cada endpoint possui um botão "Try it out" que permite:
- Preencher parâmetros
- Enviar requisições
- Ver respostas em tempo real
- Copiar exemplos de código

### 3. Visualizar Modelos de Dados
Clique em um modelo para ver sua estrutura completa:
- `SearchRequest` - Requisição de busca
- `SearchResult` - Resultado de busca
- `RAGResponse` - Resposta de RAG

## 📝 Exemplos de Uso

### Exemplo 1: Testar Busca Híbrida

1. Acesse: `https://rag-legal-app.onrender.com/api/swagger-ui.html`
2. Procure por **"Busca Híbrida"** na seção "RAG Endpoints"
3. Clique em "Try it out"
4. Preencha o JSON:
```json
{
  "query": "código penal artigo 1",
  "tribunal": "STF",
  "legalArea": "PENAL",
  "limit": 5
}
```
5. Clique em "Execute"
6. Veja a resposta em tempo real

### Exemplo 2: Upload de PDF

1. Procure por **"Upload de PDF"** na seção "PDF Upload"
2. Clique em "Try it out"
3. Clique em "Choose File" e selecione um PDF
4. Preencha os campos opcionais:
   - tribunal: STF
   - legalArea: PENAL
   - documentType: LEI
5. Clique em "Execute"
6. Veja o progresso e resultado

### Exemplo 3: Consulta RAG Síncrona

1. Procure por **"Consulta RAG Síncrona"** na seção "RAG Endpoints"
2. Clique em "Try it out"
3. Preencha o JSON:
```json
{
  "query": "Qual é a pena para roubo?",
  "tribunal": "STF",
  "legalArea": "PENAL"
}
```
4. Clique em "Execute"
5. Aguarde a resposta (pode levar alguns segundos)

## 🔍 Estrutura de Resposta

### SearchResult
```json
{
  "id": 1,
  "title": "Código Penal - Artigo 1",
  "content": "...",
  "tribunal": "STF",
  "legalArea": "PENAL",
  "score": 0.95,
  "relevanceScore": 0.92
}
```

### RAGResponse
```json
{
  "query": "Qual é a pena para roubo?",
  "response": "De acordo com o Código Penal...",
  "sourceDocuments": [
    {
      "id": 1,
      "title": "Código Penal - Artigo 157",
      "relevance": 0.98
    }
  ],
  "processingTimeMs": 1234
}
```

## 🛠️ Recursos Avançados

### 1. Copiar Exemplos de Código

Cada endpoint possui um menu "Examples" com código pronto em:
- cURL
- Python
- JavaScript
- Java

### 2. Filtrar Endpoints

Use a barra de busca no topo para filtrar por nome:
- Digite "search" para ver apenas endpoints de busca
- Digite "pdf" para ver apenas endpoints de PDF

### 3. Expandir/Recolher Seções

Clique nos títulos das seções para expandir/recolher:
- Clique em "RAG Endpoints" para expandir todos os endpoints de RAG
- Clique em "PDF Upload" para expandir todos os endpoints de PDF

### 4. Visualizar Modelos

Role até o final da página para ver a seção "Schemas" com todos os modelos de dados.

## 📱 Swagger em Diferentes Dispositivos

### Desktop
- Experiência completa com todos os recursos
- Recomendado para testes

### Mobile
- Interface responsiva
- Funciona bem em smartphones e tablets
- Alguns recursos podem ser limitados

## 🔐 Autenticação

Atualmente, a API não requer autenticação. Todos os endpoints são públicos.

**Nota:** Para produção, considere adicionar:
- API Keys
- JWT tokens
- OAuth 2.0

## 📚 Documentação Adicional

Para mais informações sobre cada endpoint, consulte:
- **README.md** - Guia principal
- **API_EXAMPLES.md** - Exemplos de requisições
- **PDF_INDEXING_GUIDE.md** - Guia de indexação de PDFs
- **ARCHITECTURE.md** - Arquitetura técnica

## 🐛 Troubleshooting

### Swagger não carrega
- Verifique se a aplicação está rodando
- Limpe o cache do navegador
- Tente em outro navegador

### Endpoints não aparecem
- Verifique se as anotações `@Operation` estão presentes
- Reinicie a aplicação
- Verifique os logs da aplicação

### Erro ao testar endpoint
- Verifique se os parâmetros estão corretos
- Veja a resposta de erro no Swagger
- Consulte os logs da aplicação

## 🚀 Próximas Melhorias

- [ ] Adicionar autenticação (API Key)
- [ ] Adicionar rate limiting
- [ ] Adicionar exemplos de resposta
- [ ] Adicionar validação de entrada
- [ ] Documentar modelos de erro

## 📞 Suporte

Para reportar problemas ou sugerir melhorias:
- Abra uma issue no GitHub
- Consulte a documentação
- Verifique os logs da aplicação

---

**Última atualização:** 2026-04-04
**Versão:** 1.0.0
