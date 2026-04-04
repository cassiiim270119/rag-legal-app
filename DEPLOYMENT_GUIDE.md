# Guia de Deploy Gratuito - RAG Legal Application

Este documento descreve as melhores opções para fazer deploy gratuito da sua aplicação Java com Docker.

## Comparativo de Plataformas Gratuitas (2026)

| Plataforma | Free Tier | Docker | Java | Limite | Observações |
|-----------|-----------|--------|------|--------|------------|
| **Render** | ✅ 750h/mês | ✅ | ✅ | 0.5GB RAM | **RECOMENDADO** |
| **Fly.io** | ✅ Limitado | ✅ | ✅ | 3 shared-cpu-1x | Bom para hobby |
| **Replit** | ✅ | ✅ | ✅ | Limitado | Bom para testes |
| **Oracle Cloud** | ✅ Always Free | ✅ | ✅ | 2 vCPU, 1GB RAM | Excelente, mas complexo |
| **Heroku** | ❌ Descontinuado | ✅ | ✅ | N/A | Não mais gratuito |
| **Railway** | ❌ Descontinuado | ✅ | ✅ | N/A | Não mais gratuito |

---

## 1. RENDER (Recomendado) ⭐

**Por que Render?**
- ✅ 750 horas/mês de Starter instances (suficiente para 1 app 24/7)
- ✅ Suporte nativo a Docker
- ✅ Deploy automático via GitHub
- ✅ SSL/HTTPS incluído
- ✅ Banco de dados PostgreSQL gratuito
- ✅ Interface intuitiva

### Passo 1: Preparar Repositório GitHub

```bash
cd /home/ubuntu/rag-legal-app

# Inicializar git
git init
git add .
git commit -m "Initial commit: RAG Legal Application"

# Criar repositório no GitHub e fazer push
git remote add origin https://github.com/seu-usuario/rag-legal-app.git
git branch -M main
git push -u origin main
```

### Passo 2: Criar Conta no Render

1. Acesse https://render.com
2. Clique em "Sign Up"
3. Autentique com GitHub
4. Autorize Render a acessar seus repositórios

### Passo 3: Criar Web Service

1. No dashboard do Render, clique em "New +"
2. Selecione "Web Service"
3. Conecte seu repositório `rag-legal-app`
4. Configure:

| Campo | Valor |
|-------|-------|
| Name | `rag-legal-app` |
| Environment | `Docker` |
| Region | `Frankfurt (Europe)` ou `Oregon (US)` |
| Branch | `main` |
| Build Command | `mvn clean install` |
| Start Command | `java -jar target/rag-legal-app-1.0.0.jar` |
| Instance Type | `Free` |

### Passo 4: Configurar Variáveis de Ambiente

1. Vá em "Environment"
2. Adicione:
```
OPENAI_API_KEY=sk-your-key-here
SPRING_PROFILES_ACTIVE=production
```

### Passo 5: Deploy

1. Clique em "Create Web Service"
2. Render começará o build automaticamente
3. Aguarde ~5-10 minutos
4. Sua app estará em: `https://rag-legal-app.onrender.com`

### Testar Deploy

```bash
# Health check
curl https://rag-legal-app.onrender.com/api/rag/health

# RAG Síncrono
curl -X POST https://rag-legal-app.onrender.com/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Código Penal"}'
```

### Limitações do Free Tier Render

- ⚠️ 0.5GB RAM (suficiente para RAG com dados pequenos)
- ⚠️ Spin-down após 15 minutos de inatividade
- ⚠️ 750 horas/mês (suficiente para 1 app 24/7)
- ⚠️ Sem suporte a WebSocket
- ⚠️ Sem garantia de uptime

**Solução:** Para produção, upgrade para Starter ($7/mês)

---

## 2. FLY.IO

**Por que Fly.io?**
- ✅ 3 shared-cpu-1x VMs gratuitas
- ✅ Deploy global automático
- ✅ Excelente performance
- ✅ Suporte a Docker nativo

### Instalação Rápida

```bash
# 1. Instalar CLI
curl -L https://fly.io/install.sh | sh

# 2. Fazer login
flyctl auth login

# 3. Criar app
cd /home/ubuntu/rag-legal-app
flyctl launch

# 4. Deploy
flyctl deploy
```

### Configurar fly.toml

```toml
app = "rag-legal-app"
primary_region = "iad"

[build]
  dockerfile = "Dockerfile"

[env]
  OPENAI_API_KEY = "sk-your-key-here"
  SPRING_PROFILES_ACTIVE = "production"

[[services]]
  protocol = "tcp"
  internal_port = 8080
  processes = ["app"]

  [[services.ports]]
    port = 80
    handlers = ["http"]

  [[services.ports]]
    port = 443
    handlers = ["tls", "http"]
```

### Deploy

```bash
flyctl deploy
```

**URL:** `https://rag-legal-app.fly.dev`

---

## 3. ORACLE CLOUD (Always Free) 🔥

**Por que Oracle Cloud?**
- ✅ Sempre gratuito (não expira)
- ✅ 2 vCPU + 1GB RAM
- ✅ Suporte a Docker
- ✅ Mais recursos que Render

### Passo 1: Criar Conta Oracle Cloud

1. Acesse https://www.oracle.com/cloud/free/
2. Clique em "Start for free"
3. Complete o registro

### Passo 2: Criar Compute Instance

1. No console, vá em "Compute" → "Instances"
2. Clique em "Create Instance"
3. Configure:
   - Image: Ubuntu 22.04
   - Shape: Ampere (Always Free eligible)
   - Network: VCN padrão
4. Clique em "Create"

### Passo 3: Configurar Instância

```bash
# SSH na instância
ssh ubuntu@seu-ip-publico

# Instalar Docker
sudo apt update
sudo apt install docker.io -y
sudo usermod -aG docker ubuntu

# Instalar Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Clonar repositório
git clone https://github.com/seu-usuario/rag-legal-app.git
cd rag-legal-app

# Build e run
docker-compose up -d
```

### Acessar Aplicação

```bash
# Verificar IP público
curl http://seu-ip-publico:8080/api/rag/health
```

---

## 4. REPLIT (Para Testes)

**Rápido para prototipagem:**

1. Acesse https://replit.com
2. Clique em "Create Repl"
3. Selecione "Docker"
4. Cole o Dockerfile
5. Clique em "Run"

**Limitação:** Spin-down após inatividade, ideal apenas para testes

---

## 5. DOCKER HUB + SELF-HOSTED

Se você tiver um servidor próprio:

### Publicar no Docker Hub

```bash
# 1. Fazer login
docker login

# 2. Build
docker build -t seu-usuario/rag-legal-app:latest .

# 3. Push
docker push seu-usuario/rag-legal-app:latest

# 4. Pull em seu servidor
docker pull seu-usuario/rag-legal-app:latest
docker run -p 8080:8080 seu-usuario/rag-legal-app:latest
```

---

## Comparativo de Custo

| Plataforma | Free | Starter | Pro |
|-----------|------|---------|-----|
| **Render** | 750h/mês | $7/mês | $25/mês |
| **Fly.io** | 3 VMs | $5/mês | $10/mês |
| **Oracle Cloud** | Sempre | - | - |
| **Heroku** | ❌ | $7/mês | $25/mês |

---

## Otimizações para Free Tier

### 1. Reduzir Tamanho da Imagem Docker

```dockerfile
# Usar Alpine em vez de Ubuntu
FROM eclipse-temurin:17-jre-alpine

# Reduzir tamanho do JAR
RUN mvn clean package -DskipTests -Dspring.profiles.active=prod
```

### 2. Limitar Memória JVM

```dockerfile
ENV JAVA_OPTS="-Xmx256m -Xms128m"
```

### 3. Desabilitar Features Não Usadas

```yaml
# application.yml
spring:
  jpa:
    show-sql: false
  h2:
    console:
      enabled: false
```

### 4. Usar Índice BM25 Comprimido

```java
// Em BM25SearchService.java
// Limitar tamanho do índice
indexWriter.setMaxBufferedDocs(100);
```

---

## Monitoramento em Produção

### Render

```bash
# Ver logs
flyctl logs

# Monitorar performance
# Dashboard → Metrics
```

### Fly.io

```bash
# Ver logs
flyctl logs

# Status
flyctl status
```

### Oracle Cloud

```bash
# SSH e monitorar
ssh ubuntu@seu-ip
docker logs -f container-id
```

---

## Troubleshooting

### Erro: "Out of Memory"

**Solução:**
```yaml
# Reduzir limite de documentos em memória
rag:
  embedding:
    max-documents: 1000
```

### Erro: "Timeout"

**Solução:**
```bash
# Aumentar timeout em application.yml
server:
  servlet:
    session:
      timeout: 30m
```

### Erro: "Build Failed"

**Solução:**
```bash
# Verificar logs localmente
mvn clean install -X

# Ou usar Docker localmente
docker build -t rag-legal-app .
docker run -p 8080:8080 rag-legal-app
```

---

## Recomendação Final

### Para Começar (Gratuito)
→ **Render** (mais fácil) ou **Fly.io** (melhor performance)

### Para Produção (Sempre Gratuito)
→ **Oracle Cloud** (mais recursos, não expira)

### Para Prototipagem Rápida
→ **Replit** (setup em 2 minutos)

---

## Próximos Passos

1. ✅ Escolher plataforma
2. ✅ Fazer push para GitHub
3. ✅ Configurar deploy automático
4. ✅ Testar endpoints
5. ✅ Monitorar logs
6. ✅ Escalar conforme necessário

---

## Recursos Úteis

- [Render Documentation](https://render.com/docs)
- [Fly.io Documentation](https://fly.io/docs/)
- [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

---

Última atualização: 2026-04-04
