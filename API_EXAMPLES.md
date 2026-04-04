# Exemplos de Requisições da API RAG

Este documento contém exemplos práticos de como usar os endpoints da aplicação RAG Legal.

## 1. Health Check

Verificar se a aplicação está rodando:

```bash
curl -X GET http://localhost:8080/api/rag/health
```

**Resposta esperada:**
```
RAG Service is running
```

---

## 2. Endpoint Síncrono (RAG Completo)

### Exemplo 1: Consulta sobre Execução Penal

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Qual a súmula sobre isolamento carcerário?",
    "tribunal": "STF",
    "legalArea": "PENAL",
    "limit": 10
  }'
```

**Resposta esperada:**
```json
{
  "query": "Qual a súmula sobre isolamento carcerário?",
  "answer": "De acordo com a Súmula 735 do STF, não se admite o isolamento carcerário do sentenciado senão por necessidade disciplinar ou de segurança penitenciária, mediante processo administrativo em que lhe seja assegurada a defesa técnica.",
  "sources": [
    {
      "id": 1,
      "documentNumber": "STF-SUM-735",
      "title": "Súmula 735 - Execução Penal",
      "content": "Não se admite o isolamento carcerário do sentenciado senão por necessidade disciplinar ou de segurança penitenciária, mediante processo administrativo em que lhe seja assegurada a defesa técnica.",
      "tribunal": "STF",
      "legalArea": "PENAL",
      "status": "VIGENTE",
      "documentType": "SUMULA",
      "publicationDate": "2003-11-13",
      "score": 0.95,
      "rerankScore": 1.045,
      "metadata": {
        "documentNumber": "STF-SUM-735",
        "documentType": "SUMULA",
        "tribunal": "STF",
        "legalArea": "PENAL",
        "status": "VIGENTE",
        "publicationDate": "2003-11-13",
        "chapter": "EXECUÇÃO PENAL",
        "article": "1",
        "paragraph": null
      }
    }
  ],
  "sourceCount": 1,
  "processingTimeMs": 1245
}
```

### Exemplo 2: Consulta sobre Direito Civil

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "O que diz a lei sobre direitos autorais?",
    "tribunal": "STJ",
    "legalArea": "CIVIL"
  }'
```

### Exemplo 3: Consulta sem Filtros de Metadados

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Código Penal artigo 1"
  }'
```

---

## 3. Endpoint com Streaming

Receber respostas em tempo real (text/event-stream):

### Exemplo 1: Stream com Filtro de Tribunal

```bash
curl -X POST http://localhost:8080/api/rag/query-stream \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Quais são os princípios fundamentais do direito penal?",
    "tribunal": "STF",
    "legalArea": "PENAL"
  }' \
  --no-buffer
```

**Resposta esperada (streaming):**
```
De acordo com o Código Penal Brasileiro...
Art. 1º - Não há crime sem lei anterior...
O princípio da legalidade é fundamental...
```

### Exemplo 2: Stream sem Filtros

```bash
curl -X POST http://localhost:8080/api/rag/query-stream \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Lei de Acesso à Informação"
  }' \
  --no-buffer
```

### Exemplo 3: Stream com Python

```python
import requests
import json

url = "http://localhost:8080/api/rag/query-stream"
payload = {
    "query": "Direito autoral e propriedade intelectual",
    "legalArea": "CIVIL"
}

response = requests.post(url, json=payload, stream=True)

for chunk in response.iter_content(decode_unicode=True):
    if chunk:
        print(chunk, end='', flush=True)
```

### Exemplo 4: Stream com JavaScript/Node.js

```javascript
const query = {
  query: "Execução penal e direitos do condenado",
  tribunal: "STF"
};

const response = await fetch('http://localhost:8080/api/rag/query-stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(query)
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  console.log(decoder.decode(value));
}
```

---

## 4. Endpoint de Busca Híbrida (sem LLM)

Apenas busca, sem geração de resposta:

### Exemplo 1: Busca por Termo Específico

```bash
curl -X POST http://localhost:8080/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "código penal",
    "limit": 5
  }'
```

**Resposta esperada:**
```json
[
  {
    "id": 2,
    "documentNumber": "LEI-1940-CP",
    "title": "Lei 2.848/1940 - Código Penal Brasileiro",
    "content": "Art. 1º - Não há crime sem lei anterior que o defina...",
    "tribunal": "STF",
    "legalArea": "PENAL",
    "status": "VIGENTE",
    "documentType": "LEI",
    "publicationDate": "1940-12-07",
    "score": 0.98,
    "rerankScore": 1.078,
    "metadata": {...}
  },
  {
    "id": 5,
    "documentNumber": "LEI-7210-LEP",
    "title": "Lei 7.210/1984 - Lei de Execução Penal",
    "content": "Art. 1º - A execução penal tem por objetivo...",
    "tribunal": "STF",
    "legalArea": "PENAL",
    "status": "VIGENTE",
    "documentType": "LEI",
    "publicationDate": "1984-07-11",
    "score": 0.87,
    "rerankScore": 0.957,
    "metadata": {...}
  }
]
```

### Exemplo 2: Busca com Filtro de Tribunal

```bash
curl -X POST http://localhost:8080/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "direito civil",
    "tribunal": "STJ",
    "legalArea": "CIVIL",
    "limit": 3
  }'
```

### Exemplo 3: Busca com Limite Customizado

```bash
curl -X POST http://localhost:8080/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "lei de acesso",
    "limit": 20
  }'
```

---

## 5. Usando com Postman

### Importar Collection

1. Abra o Postman
2. Clique em "Import"
3. Cole a URL ou importe o arquivo JSON abaixo

### Collection JSON

```json
{
  "info": {
    "name": "RAG Legal API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/rag/health"
      }
    },
    {
      "name": "RAG Síncrono",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "url": "http://localhost:8080/api/rag/query",
        "body": {
          "mode": "raw",
          "raw": "{\"query\": \"Qual a súmula sobre isolamento carcerário?\", \"tribunal\": \"STF\", \"legalArea\": \"PENAL\"}"
        }
      }
    },
    {
      "name": "RAG com Stream",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "url": "http://localhost:8080/api/rag/query-stream",
        "body": {
          "mode": "raw",
          "raw": "{\"query\": \"Direito autoral\", \"legalArea\": \"CIVIL\"}"
        }
      }
    },
    {
      "name": "Busca Híbrida",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "url": "http://localhost:8080/api/rag/search",
        "body": {
          "mode": "raw",
          "raw": "{\"query\": \"código penal\", \"limit\": 5}"
        }
      }
    }
  ]
}
```

---

## 6. Usando com cURL (Exemplos Avançados)

### Salvar Resposta em Arquivo

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Código Civil"}' \
  -o response.json
```

### Com Timeout

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Lei de Execução Penal"}' \
  --max-time 10
```

### Com Autenticação (quando implementado)

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"query": "Direito Administrativo"}'
```

### Verbose (Debug)

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "STF"}' \
  -v
```

---

## 7. Tratamento de Erros

### Erro 400 - Bad Request (Query vazia)

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": ""}'
```

**Resposta:**
```
HTTP/1.1 400 Bad Request
```

### Erro 500 - Internal Server Error

```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Consulta que causa erro"}'
```

**Resposta esperada:**
```json
{
  "error": "Erro ao processar sua consulta: ..."
}
```

---

## 8. Performance e Limites

| Métrica | Valor |
|---------|-------|
| Limite máximo de resultados | 50 |
| Timeout padrão | 30 segundos |
| Tamanho máximo de query | 5000 caracteres |
| Máximo de documentos no contexto | 10 |
| Reranking (Top-K) | 5 resultados |

---

## 9. Dicas de Uso

### Melhor Performance
- Use filtros de `tribunal` e `legalArea` quando possível
- Limite resultados com o parâmetro `limit`
- Prefira streaming para respostas longas

### Melhor Precisão
- Seja específico na query (ex: "STF-SUM-735" em vez de "súmula")
- Use termos jurídicos corretos
- Combine múltiplas buscas se necessário

### Desenvolvimento
- Use o endpoint `/search` para testar buscas sem LLM
- Monitore os `scores` e `rerankScore` para entender relevância
- Verifique os `metadata` para validar filtros

---

## 10. Troubleshooting

### Erro: "Connection refused"
```bash
# Verificar se a aplicação está rodando
curl http://localhost:8080/api/rag/health
```

### Erro: "No documents found"
- Verifique se os dados foram carregados
- Tente uma query mais genérica
- Remova filtros de `tribunal` ou `legalArea`

### Resposta lenta
- Reduza o `limit`
- Use filtros de metadados
- Verifique logs da aplicação

---

## 11. Integração com Frontend

### JavaScript/React

```javascript
async function queryRAG(query, tribunal, legalArea) {
  const response = await fetch('http://localhost:8080/api/rag/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, tribunal, legalArea })
  });
  return response.json();
}

// Uso
const result = await queryRAG('Código Penal', 'STF', 'PENAL');
console.log(result.answer);
```

### Python

```python
import requests

def query_rag(query, tribunal=None, legal_area=None):
    url = 'http://localhost:8080/api/rag/query'
    payload = {
        'query': query,
        'tribunal': tribunal,
        'legalArea': legal_area
    }
    response = requests.post(url, json=payload)
    return response.json()

# Uso
result = query_rag('Lei de Acesso à Informação', legal_area='ADMINISTRATIVO')
print(result['answer'])
```

---

Última atualização: 2026-04-04
