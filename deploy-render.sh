#!/bin/bash

# Script de Deploy Rápido para Render
# Uso: ./deploy-render.sh

set -e

echo "🚀 RAG Legal App - Deploy para Render"
echo "======================================"

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar pré-requisitos
echo -e "${YELLOW}1. Verificando pré-requisitos...${NC}"

if ! command -v git &> /dev/null; then
    echo -e "${RED}❌ Git não está instalado${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven não está instalado${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Git e Maven encontrados${NC}"

# Verificar repositório Git
echo -e "${YELLOW}2. Verificando repositório Git...${NC}"

if [ ! -d ".git" ]; then
    echo -e "${YELLOW}Inicializando repositório Git...${NC}"
    git init
    git add .
    git commit -m "Initial commit: RAG Legal Application"
else
    echo -e "${GREEN}✅ Repositório Git já existe${NC}"
fi

# Build local
echo -e "${YELLOW}3. Fazendo build local...${NC}"
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Build bem-sucedido${NC}"
else
    echo -e "${RED}❌ Build falhou${NC}"
    exit 1
fi

# Verificar Dockerfile
echo -e "${YELLOW}4. Verificando Dockerfile...${NC}"

if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}❌ Dockerfile não encontrado${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Dockerfile encontrado${NC}"

# Instruções finais
echo ""
echo -e "${GREEN}✅ Aplicação pronta para deploy!${NC}"
echo ""
echo -e "${YELLOW}Próximos passos:${NC}"
echo "1. Fazer push para GitHub:"
echo "   git remote add origin https://github.com/seu-usuario/rag-legal-app.git"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "2. Acessar https://render.com"
echo "3. Conectar seu repositório GitHub"
echo "4. Criar novo Web Service com:"
echo "   - Environment: Docker"
echo "   - Build Command: mvn clean install"
echo "   - Start Command: java -jar target/rag-legal-app-1.0.0.jar"
echo ""
echo "5. Adicionar variáveis de ambiente:"
echo "   - OPENAI_API_KEY=sk-your-key-here"
echo "   - SPRING_PROFILES_ACTIVE=production"
echo ""
echo -e "${YELLOW}Documentação completa em: DEPLOYMENT_GUIDE.md${NC}"
