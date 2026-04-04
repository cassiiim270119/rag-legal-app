#!/bin/bash

# Script de Testes para Deploy no Render
# Uso: ./test-deployment.sh https://rag-legal-app.onrender.com

set -e

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Verificar URL
if [ -z "$1" ]; then
    echo -e "${RED}❌ Erro: URL não fornecida${NC}"
    echo "Uso: ./test-deployment.sh <URL>"
    echo "Exemplo: ./test-deployment.sh https://rag-legal-app.onrender.com"
    exit 1
fi

API_URL="$1"
RESULTS_FILE="test-results-$(date +%s).txt"

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     RAG Legal App - Testes de Deployment              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}URL da API: ${NC}${API_URL}"
echo -e "${YELLOW}Resultados serão salvos em: ${NC}${RESULTS_FILE}"
echo ""

# Função para testar endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Teste: ${description}${NC}"
    echo -e "${YELLOW}Endpoint: ${method} ${endpoint}${NC}"
    
    if [ -n "$data" ]; then
        echo -e "${YELLOW}Payload: ${data:0:100}...${NC}"
    fi
    echo ""
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "${API_URL}${endpoint}")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "${API_URL}${endpoint}")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo -e "${GREEN}✅ Status: ${http_code}${NC}"
        echo -e "${GREEN}Resposta:${NC}"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        echo ""
        echo "✅ PASSOU" >> "$RESULTS_FILE"
    else
        echo -e "${RED}❌ Status: ${http_code}${NC}"
        echo -e "${RED}Resposta:${NC}"
        echo "$body"
        echo ""
        echo "❌ FALHOU (HTTP $http_code)" >> "$RESULTS_FILE"
    fi
    
    echo ""
}

# Iniciar testes
echo -e "${YELLOW}Iniciando testes...${NC}"
echo "" > "$RESULTS_FILE"
echo "Testes de Deployment - $(date)" >> "$RESULTS_FILE"
echo "URL: $API_URL" >> "$RESULTS_FILE"
echo "" >> "$RESULTS_FILE"

# Teste 1: Health Check
echo -e "${BLUE}[1/5] Health Check${NC}"
test_endpoint "GET" "/api/rag/health" "" "Verificar se a aplicação está rodando"

# Teste 2: Busca Híbrida Simples
echo -e "${BLUE}[2/5] Busca Híbrida${NC}"
test_endpoint "POST" "/api/rag/search" \
    '{"query": "código penal", "limit": 3}' \
    "Busca por 'código penal'"

# Teste 3: RAG Síncrono
echo -e "${BLUE}[3/5] RAG Síncrono${NC}"
test_endpoint "POST" "/api/rag/query" \
    '{"query": "O que diz o código penal sobre crime?", "tribunal": "STF", "legalArea": "PENAL"}' \
    "Consulta RAG sobre Código Penal"

# Teste 4: Busca por Tribunal
echo -e "${BLUE}[4/5] Busca por Tribunal${NC}"
test_endpoint "POST" "/api/rag/search" \
    '{"query": "direito civil", "tribunal": "STJ", "legalArea": "CIVIL", "limit": 5}' \
    "Busca por 'direito civil' no STJ"

# Teste 5: RAG com Filtros
echo -e "${BLUE}[5/5] RAG com Filtros${NC}"
test_endpoint "POST" "/api/rag/query" \
    '{"query": "Lei de Acesso à Informação", "legalArea": "ADMINISTRATIVO"}' \
    "Consulta sobre Lei de Acesso à Informação"

# Resumo
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    RESUMO DOS TESTES                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

passed=$(grep -c "✅ PASSOU" "$RESULTS_FILE" || true)
failed=$(grep -c "❌ FALHOU" "$RESULTS_FILE" || true)
total=$((passed + failed))

echo -e "${GREEN}✅ Testes Passaram: ${passed}/${total}${NC}"
echo -e "${RED}❌ Testes Falharam: ${failed}/${total}${NC}"
echo ""

if [ "$failed" -eq 0 ]; then
    echo -e "${GREEN}🎉 Todos os testes passaram! Aplicação está funcionando corretamente.${NC}"
    exit 0
else
    echo -e "${RED}⚠️  Alguns testes falharam. Verifique os logs acima.${NC}"
    exit 1
fi
