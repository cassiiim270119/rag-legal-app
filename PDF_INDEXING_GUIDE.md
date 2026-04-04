# Guia de Indexação de PDFs - RAG Legal App

## 📋 Visão Geral

A aplicação RAG Legal agora suporta upload e indexação automática de documentos PDF. Os PDFs são processados para extrair texto, divididos em chunks inteligentes, e indexados para busca híbrida (BM25 + Vetorial).

## 🚀 Recursos

- ✅ **Extração de Texto**: Suporta PDFs com texto selecionável
- ✅ **Extração de Metadados**: Título, autor, data de criação, etc
- ✅ **Chunking Inteligente**: Divide por parágrafos mantendo semântica
- ✅ **Geração de Embeddings**: Automática para cada chunk
- ✅ **Indexação BM25**: Busca full-text rápida
- ✅ **Upload em Batch**: Processa múltiplos PDFs simultaneamente
- ✅ **Validação**: Verifica tipo e tamanho do arquivo

## 📊 Arquitetura

```
PDF Upload
    ↓
PDFExtractionService (extrai texto e metadados)
    ↓
Chunking (divide em partes semânticas)
    ↓
EmbeddingService (gera embeddings 384-dim)
    ↓
BM25SearchService (indexa para busca full-text)
    ↓
LegalDocumentRepository (persiste no banco)
    ↓
Pronto para Busca Híbrida e RAG
```

## 🔌 Endpoints

### 1. Upload de Um Único PDF

**Endpoint:** `POST /api/pdf/upload`

**Parâmetros:**
- `file` (obrigatório): Arquivo PDF
- `tribunal` (opcional): STF, STJ, TRF, TJSP, etc
- `legalArea` (opcional): PENAL, CIVIL, TRABALHISTA, ADMINISTRATIVO, etc
- `documentType` (opcional): SUMULA, LEI, DECRETO, RESOLUCAO, etc

**Exemplo com cURL:**
```bash
curl -X POST https://rag-legal-app.onrender.com/api/pdf/upload \
  -F "file=@codigo_penal.pdf" \
  -F "tribunal=STF" \
  -F "legalArea=PENAL" \
  -F "documentType=LEI"
```

**Exemplo com Python:**
```python
import requests

with open('codigo_penal.pdf', 'rb') as f:
    files = {'file': f}
    data = {
        'tribunal': 'STF',
        'legalArea': 'PENAL',
        'documentType': 'LEI'
    }
    response = requests.post(
        'https://rag-legal-app.onrender.com/api/pdf/upload',
        files=files,
        data=data
    )
    print(response.json())
```

**Exemplo com JavaScript/Node.js:**
```javascript
const FormData = require('form-data');
const fs = require('fs');
const axios = require('axios');

const form = new FormData();
form.append('file', fs.createReadStream('codigo_penal.pdf'));
form.append('tribunal', 'STF');
form.append('legalArea', 'PENAL');
form.append('documentType', 'LEI');

axios.post('https://rag-legal-app.onrender.com/api/pdf/upload', form, {
  headers: form.getHeaders()
}).then(res => console.log(res.data));
```

**Resposta de Sucesso (200):**
```json
{
  "success": true,
  "message": "PDF indexado com sucesso",
  "fileName": "codigo_penal.pdf",
  "fileSize": 1024000,
  "chunksCreated": 45,
  "totalCharacters": 250000,
  "pageCount": "150",
  "processingTimeMs": 2345,
  "documents": [
    {
      "id": 1,
      "title": "Código Penal (Parte 1)",
      "pageNumber": 1
    },
    {
      "id": 2,
      "title": "Código Penal (Parte 2)",
      "pageNumber": 2
    }
  ]
}
```

**Resposta de Erro (400/500):**
```json
{
  "success": false,
  "message": "Erro ao processar PDF: [detalhes do erro]"
}
```

### 2. Upload em Batch (Múltiplos PDFs)

**Endpoint:** `POST /api/pdf/upload-batch`

**Parâmetros:**
- `files` (obrigatório): Lista de arquivos PDF
- `tribunal` (opcional): Aplicado a todos os arquivos
- `legalArea` (opcional): Aplicado a todos os arquivos
- `documentType` (opcional): Aplicado a todos os arquivos

**Exemplo com cURL:**
```bash
curl -X POST https://rag-legal-app.onrender.com/api/pdf/upload-batch \
  -F "files=@lei1.pdf" \
  -F "files=@lei2.pdf" \
  -F "files=@lei3.pdf" \
  -F "tribunal=STF" \
  -F "legalArea=PENAL"
```

**Exemplo com Python:**
```python
import requests

files = [
    ('files', open('lei1.pdf', 'rb')),
    ('files', open('lei2.pdf', 'rb')),
    ('files', open('lei3.pdf', 'rb'))
]
data = {
    'tribunal': 'STF',
    'legalArea': 'PENAL'
}

response = requests.post(
    'https://rag-legal-app.onrender.com/api/pdf/upload-batch',
    files=files,
    data=data
)
print(response.json())
```

**Resposta de Sucesso (200):**
```json
{
  "totalFiles": 3,
  "successCount": 3,
  "errorCount": 0,
  "processingTimeMs": 5678,
  "results": [
    {
      "success": true,
      "fileName": "lei1.pdf",
      "chunksCreated": 45
    },
    {
      "success": true,
      "fileName": "lei2.pdf",
      "chunksCreated": 32
    },
    {
      "success": true,
      "fileName": "lei3.pdf",
      "chunksCreated": 28
    }
  ]
}
```

### 3. Estatísticas de Indexação

**Endpoint:** `GET /api/pdf/stats`

**Exemplo:**
```bash
curl https://rag-legal-app.onrender.com/api/pdf/stats
```

**Resposta:**
```json
{
  "totalDocuments": 150,
  "pdfDocuments": 120,
  "indexedPercentage": 80.0
}
```

### 4. Health Check

**Endpoint:** `GET /api/pdf/health`

**Exemplo:**
```bash
curl https://rag-legal-app.onrender.com/api/pdf/health
```

**Resposta:**
```json
{
  "status": "UP",
  "service": "PDF Upload and Indexing Service",
  "timestamp": 1712345678901
}
```

## 🔧 Configuração

### Limites de Upload

- **Tamanho máximo por arquivo**: 50 MB
- **Tamanho máximo de chunk**: 500 caracteres
- **Sobreposição de chunk**: 100 caracteres

Para alterar estes valores, edite `PDFIndexingService.java`:

```java
private static final int CHUNK_SIZE = 500; // Altere aqui
private static final int CHUNK_OVERLAP = 100; // Altere aqui
```

### Tipos de Documentos Suportados

- SUMULA
- LEI
- DECRETO
- RESOLUCAO
- PORTARIA
- INSTRUCAO
- PARECER
- SENTENCA
- ACORDAO
- PDF (padrão)

## 📈 Fluxo de Processamento

### 1. Extração de Texto
- Usa Apache PDFBox para extrair texto
- Suporta PDFs com texto selecionável
- PDFs com apenas imagens não funcionam

### 2. Extração de Metadados
- Título do PDF
- Autor
- Data de criação
- Número de páginas
- Tamanho do arquivo

### 3. Limpeza de Texto
- Remove caracteres especiais problemáticos
- Normaliza espaços em branco
- Remove linhas vazias extras

### 4. Chunking Inteligente
- Divide por parágrafos (mais semântico que por caracteres)
- Mantém contexto com sobreposição
- Ignora chunks muito pequenos (< 50 caracteres)

### 5. Geração de Embeddings
- Usa modelo local all-minilm-l6-v2 (384 dimensões)
- Determinístico (mesmo texto = mesmo embedding)
- Sem custos de API

### 6. Indexação BM25
- Indexação full-text para busca rápida
- Permite busca por palavras-chave
- Combinado com busca vetorial para resultados melhores

### 7. Persistência
- Salva cada chunk como documento separado
- Mantém referência ao arquivo original
- Armazena número da página/chunk

## 🔍 Buscando Documentos Indexados

Após indexar PDFs, você pode buscá-los usando os endpoints de RAG:

### Busca Híbrida
```bash
curl -X POST https://rag-legal-app.onrender.com/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "artigo 1 código penal",
    "tribunal": "STF",
    "legalArea": "PENAL",
    "limit": 5
  }'
```

### RAG Síncrono
```bash
curl -X POST https://rag-legal-app.onrender.com/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Qual é a pena para roubo?",
    "tribunal": "STF",
    "legalArea": "PENAL"
  }'
```

## ⚠️ Limitações

1. **Apenas PDFs com texto**: PDFs que são apenas imagens (scans) não funcionam
2. **Tamanho máximo**: 50 MB por arquivo
3. **Idioma**: Otimizado para português, mas funciona com outros idiomas
4. **Caracteres especiais**: Alguns caracteres especiais podem ser removidos

## 🐛 Troubleshooting

### Erro: "Apenas arquivos PDF são aceitos"
- Verifique se o arquivo é realmente um PDF
- Certifique-se de que o `Content-Type` é `application/pdf`

### Erro: "Arquivo muito grande"
- Divida o PDF em partes menores (< 50 MB)
- Use ferramentas como `pdftk` ou `ghostscript`

### Erro: "Nenhum conteúdo extraído"
- O PDF pode ser apenas imagens (scan)
- Tente usar OCR antes de enviar

### Chunks não aparecem na busca
- Aguarde alguns segundos após o upload
- Verifique se o tribunal/legalArea estão corretos
- Use `/api/pdf/stats` para confirmar indexação

## 📚 Exemplos Completos

### Exemplo 1: Upload e Busca Imediata

```bash
# 1. Upload do PDF
curl -X POST https://rag-legal-app.onrender.com/api/pdf/upload \
  -F "file=@lei.pdf" \
  -F "tribunal=STF" \
  -F "legalArea=PENAL"

# 2. Aguarde 2-3 segundos

# 3. Busque o conteúdo
curl -X POST https://rag-legal-app.onrender.com/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{"query": "artigo", "limit": 5}'
```

### Exemplo 2: Batch Upload com Python

```python
import requests
import os
from pathlib import Path

pdf_dir = './documentos'
pdf_files = list(Path(pdf_dir).glob('*.pdf'))

files = [('files', open(pdf, 'rb')) for pdf in pdf_files]
data = {
    'tribunal': 'STF',
    'legalArea': 'PENAL',
    'documentType': 'LEI'
}

response = requests.post(
    'https://rag-legal-app.onrender.com/api/pdf/upload-batch',
    files=files,
    data=data
)

result = response.json()
print(f"Sucesso: {result['successCount']}")
print(f"Erros: {result['errorCount']}")
print(f"Tempo: {result['processingTimeMs']}ms")
```

## 🎯 Próximos Passos

1. **Adicionar OCR**: Para PDFs com imagens
2. **Suporte a Tabelas**: Extrair tabelas estruturadas
3. **Análise de Estrutura**: Detectar seções automaticamente
4. **Versionamento**: Rastrear versões de documentos
5. **Atualizações**: Atualizar documentos existentes

## 📞 Suporte

Para reportar bugs ou sugerir melhorias, abra uma issue no repositório GitHub:
https://github.com/cassiiim270119/rag-legal-app
