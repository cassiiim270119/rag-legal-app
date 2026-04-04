#!/bin/bash

# Script de Deploy Rápido para Fly.io
# Uso: ./deploy-flyio.sh

set -e

echo "🚀 RAG Legal App - Deploy para Fly.io"
echo "======================================"

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar pré-requisitos
echo -e "${YELLOW}1. Verificando pré-requisitos...${NC}"

if ! command -v flyctl &> /dev/null; then
    echo -e "${YELLOW}Instalando Fly CLI...${NC}"
    curl -L https://fly.io/install.sh | sh
    export PATH="$PATH:$HOME/.fly/bin"
fi

echo -e "${GREEN}✅ Fly CLI encontrado${NC}"

# Fazer login
echo -e "${YELLOW}2. Verificando autenticação Fly.io...${NC}"

if ! flyctl auth whoami &> /dev/null; then
    echo -e "${YELLOW}Faça login no Fly.io:${NC}"
    flyctl auth login
fi

echo -e "${GREEN}✅ Autenticado no Fly.io${NC}"

# Build local
echo -e "${YELLOW}3. Fazendo build local...${NC}"
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Build bem-sucedido${NC}"
else
    echo -e "${RED}❌ Build falhou${NC}"
    exit 1
fi

# Criar app no Fly.io
echo -e "${YELLOW}4. Criando/Atualizando app no Fly.io...${NC}"

if [ ! -f "fly.toml" ]; then
    echo -e "${YELLOW}Gerando fly.toml...${NC}"
    flyctl launch --no-deploy
else
    echo -e "${GREEN}✅ fly.toml já existe${NC}"
fi

# Deploy
echo -e "${YELLOW}5. Fazendo deploy...${NC}"
flyctl deploy

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Deploy bem-sucedido!${NC}"
    
    # Obter URL
    APP_NAME=$(grep '^app = ' fly.toml | cut -d'"' -f2)
    echo ""
    echo -e "${GREEN}✅ Aplicação disponível em:${NC}"
    echo "   https://${APP_NAME}.fly.dev"
    echo ""
    echo -e "${YELLOW}Próximos passos:${NC}"
    echo "1. Adicionar variável de ambiente:"
    echo "   flyctl secrets set OPENAI_API_KEY=sk-your-key-here"
    echo ""
    echo "2. Monitorar logs:"
    echo "   flyctl logs"
    echo ""
    echo "3. Testar saúde:"
    echo "   curl https://${APP_NAME}.fly.dev/api/rag/health"
else
    echo -e "${RED}❌ Deploy falhou${NC}"
    exit 1
fi
