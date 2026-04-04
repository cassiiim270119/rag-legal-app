# Guia de Testes - RAG Legal Application

## Após o Deploy no Render

Quando seu deploy no Render estiver completo, você receberá uma URL como:
```
https://rag-legal-app.onrender.com
```

## Testes Rápidos

### 1. Health Check (Verificar se está online)

```bash
curl https://rag-legal-app.onrender.com/api/rag/health
```

**Resposta esperada:**
```
RAG Service is running
```

---

## Testes Completos com Script

Execute o script de testes automatizado:

```bash
./test-deployment.sh https://rag-legal-app.onrender.com
```

Isso executará 5 testes:
1. ✅ Health Check
2. ✅ Busca Híbrida
3. ✅ RAG Síncrono
4. ✅ Busca por Tribunal
5. ✅ RAG com Filtros

---

## Testes Manuais

### Teste 1: Busca Simples

```bash
curl -X POST https://rag-legal-app.onrender.com/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "código penal",
    "limit": 3
  }'
```

**Resposta esperada:** Array com documentos encontrados

---

### Teste 2: RAG Síncrono (com resposta do LLM)

```bash
curl -X POST https://rag-legal-app.onrender.com/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "O que diz o código penal sobre crime?",
    "tribunal": "STF",
    "legalArea": "PENAL"
  }'
```

**Resposta esperada:** JSON com:
- `query`: Sua pergunta
- `answer`: Resposta gerada (simulada ou com Groq)
- `sources`: Array com documentos encontrados
- `sourceCount`: Número de fontes
- `processingTimeMs`: Tempo de processamento

---

### Teste 3: RAG com Streaming

```bash
curl -X POST https://rag-legal-app.onrender.com/api/rag/query-stream \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Lei de Acesso à Informação",
    "legalArea": "ADMINISTRATIVO"
  }' \
  --no-buffer
```

**Resposta esperada:** Stream de texto em tempo real

---

### Teste 4: Busca com Filtros

```bash
curl -X POST https://rag-legal-app.onrender.com/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "direito civil",
    "tribunal": "STJ",
    "legalArea": "CIVIL",
    "limit": 5
  }'
```

---

### Teste 5: Busca por Área Legal

```bash
curl -X POST https://rag-legal-app.onrender.com/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "execução penal",
    "legalArea": "PENAL",
    "limit": 10
  }'
```

---

## Testes com Python

### Script de Teste Completo

```python
import requests
import json
import time

BASE_URL = "https://rag-legal-app.onrender.com/api"

def test_health():
    """Teste 1: Health Check"""
    print("🔍 Teste 1: Health Check")
    response = requests.get(f"{BASE_URL}/rag/health")
    print(f"Status: {response.status_code}")
    print(f"Resposta: {response.text}\n")
    return response.status_code == 200

def test_search():
    """Teste 2: Busca Híbrida"""
    print("🔍 Teste 2: Busca Híbrida")
    payload = {
        "query": "código penal",
        "limit": 3
    }
    response = requests.post(f"{BASE_URL}/rag/search", json=payload)
    print(f"Status: {response.status_code}")
    print(f"Documentos encontrados: {len(response.json())}\n")
    return response.status_code == 200

def test_rag_sync():
    """Teste 3: RAG Síncrono"""
    print("🔍 Teste 3: RAG Síncrono")
    payload = {
        "query": "O que diz o código penal sobre crime?",
        "tribunal": "STF",
        "legalArea": "PENAL"
    }
    start = time.time()
    response = requests.post(f"{BASE_URL}/rag/query", json=payload)
    elapsed = time.time() - start
    
    print(f"Status: {response.status_code}")
    data = response.json()
    print(f"Pergunta: {data.get('query')}")
    print(f"Resposta: {data.get('answer')[:100]}...")
    print(f"Fontes: {data.get('sourceCount')}")
    print(f"Tempo: {elapsed:.2f}s\n")
    return response.status_code == 200

def test_rag_stream():
    """Teste 4: RAG com Streaming"""
    print("🔍 Teste 4: RAG com Streaming")
    payload = {
        "query": "Lei de Acesso à Informação",
        "legalArea": "ADMINISTRATIVO"
    }
    response = requests.post(f"{BASE_URL}/rag/query-stream", json=payload, stream=True)
    print(f"Status: {response.status_code}")
    print("Resposta (streaming):")
    for chunk in response.iter_content(decode_unicode=True):
        if chunk:
            print(chunk, end='', flush=True)
    print("\n")
    return response.status_code == 200

def test_search_with_filters():
    """Teste 5: Busca com Filtros"""
    print("🔍 Teste 5: Busca com Filtros")
    payload = {
        "query": "direito civil",
        "tribunal": "STJ",
        "legalArea": "CIVIL",
        "limit": 5
    }
    response = requests.post(f"{BASE_URL}/rag/search", json=payload)
    print(f"Status: {response.status_code}")
    print(f"Documentos encontrados: {len(response.json())}\n")
    return response.status_code == 200

if __name__ == "__main__":
    print("=" * 60)
    print("RAG Legal App - Testes de Deployment")
    print("=" * 60)
    print()
    
    tests = [
        test_health,
        test_search,
        test_rag_sync,
        test_rag_stream,
        test_search_with_filters
    ]
    
    results = []
    for test in tests:
        try:
            result = test()
            results.append(result)
        except Exception as e:
            print(f"❌ Erro: {e}\n")
            results.append(False)
    
    print("=" * 60)
    print(f"Resultados: {sum(results)}/{len(results)} testes passaram")
    print("=" * 60)
```

**Executar:**
```bash
python3 test_rag_api.py
```

---

## Testes com JavaScript/Node.js

```javascript
const BASE_URL = "https://rag-legal-app.onrender.com/api";

async function testHealth() {
  console.log("🔍 Teste 1: Health Check");
  const response = await fetch(`${BASE_URL}/rag/health`);
  console.log(`Status: ${response.status}`);
  console.log(`Resposta: ${await response.text()}\n`);
  return response.ok;
}

async function testSearch() {
  console.log("🔍 Teste 2: Busca Híbrida");
  const payload = {
    query: "código penal",
    limit: 3
  };
  const response = await fetch(`${BASE_URL}/rag/search`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await response.json();
  console.log(`Status: ${response.status}`);
  console.log(`Documentos encontrados: ${data.length}\n`);
  return response.ok;
}

async function testRagSync() {
  console.log("🔍 Teste 3: RAG Síncrono");
  const payload = {
    query: "O que diz o código penal sobre crime?",
    tribunal: "STF",
    legalArea: "PENAL"
  };
  const start = Date.now();
  const response = await fetch(`${BASE_URL}/rag/query`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await response.json();
  const elapsed = (Date.now() - start) / 1000;
  
  console.log(`Status: ${response.status}`);
  console.log(`Pergunta: ${data.query}`);
  console.log(`Resposta: ${data.answer.substring(0, 100)}...`);
  console.log(`Fontes: ${data.sourceCount}`);
  console.log(`Tempo: ${elapsed.toFixed(2)}s\n`);
  return response.ok;
}

async function runTests() {
  console.log("=".repeat(60));
  console.log("RAG Legal App - Testes de Deployment");
  console.log("=".repeat(60));
  console.log();
  
  const tests = [testHealth, testSearch, testRagSync];
  const results = [];
  
  for (const test of tests) {
    try {
      const result = await test();
      results.push(result);
    } catch (error) {
      console.log(`❌ Erro: ${error}\n`);
      results.push(false);
    }
  }
  
  console.log("=".repeat(60));
  console.log(`Resultados: ${results.filter(r => r).length}/${results.length} testes passaram`);
  console.log("=".repeat(60));
}

runTests();
```

---

## Troubleshooting

### Erro: "Connection refused"
- A aplicação ainda está iniciando no Render
- Aguarde 5-10 minutos e tente novamente
- Verifique o status em https://render.com

### Erro: "GROQ_API_KEY not configured"
- Isso é esperado se você não configurou a variável de ambiente
- A aplicação funcionará em modo simulado (sem LLM)
- Para usar LLM, adicione a variável GROQ_API_KEY no Render

### Erro: "No documents found"
- Os dados de exemplo estão sendo carregados
- Aguarde alguns segundos e tente novamente
- Verifique os logs no Render

### Resposta lenta
- Free tier do Render tem recursos limitados
- Primeira requisição pode ser mais lenta (cold start)
- Reduza o `limit` para melhor performance

---

## Próximas Etapas

1. ✅ Verificar que a API está respondendo
2. ✅ Testar endpoints básicos
3. ⏭️ Adicionar GROQ_API_KEY para usar LLM
4. ⏭️ Monitorar logs no Render
5. ⏭️ Escalar para plano pago se necessário

---

## Recursos Úteis

- [Render Documentation](https://render.com/docs)
- [Groq API Documentation](https://console.groq.com/docs)
- [RAG API Examples](./API_EXAMPLES.md)

---

Última atualização: 2026-04-04
