# Guia de Desenvolvimento - RAG Legal Application

## Configuração do Ambiente

### Pré-requisitos
- Java 17 ou superior
- Maven 3.8+
- Git
- IDE (IntelliJ IDEA, Eclipse ou VS Code)
- OpenAI API Key (opcional, para testes com LLM real)

### Setup Inicial

1. **Clone o repositório**
```bash
git clone <repository-url>
cd rag-legal-app
```

2. **Configure a API Key do OpenAI** (opcional)
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

## Estrutura de Diretórios

```
rag-legal-app/
├── src/
│   ├── main/
│   │   ├── java/com/rag/legal/
│   │   │   ├── RagLegalApplication.java       # Entry point
│   │   │   ├── controller/
│   │   │   │   └── RAGController.java         # REST endpoints
│   │   │   ├── domain/
│   │   │   │   └── LegalDocument.java         # Entity JPA
│   │   │   ├── dto/
│   │   │   │   ├── SearchRequest.java
│   │   │   │   ├── SearchResult.java
│   │   │   │   └── RAGResponse.java
│   │   │   ├── repository/
│   │   │   │   └── LegalDocumentRepository.java
│   │   │   ├── service/
│   │   │   │   ├── EmbeddingService.java      # Embeddings locais
│   │   │   │   ├── BM25SearchService.java     # Busca por palavras-chave
│   │   │   │   ├── HybridSearchService.java   # Busca híbrida + reranking
│   │   │   │   ├── RAGService.java            # Orquestração RAG
│   │   │   │   └── DataInitializationService.java
│   │   │   └── config/
│   │   │       └── AppConfig.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/rag/legal/
│           └── (testes unitários)
├── pom.xml
├── README.md
├── ARCHITECTURE.md
├── API_EXAMPLES.md
├── DEVELOPMENT.md
├── Dockerfile
└── docker-compose.yml
```

## Fluxo de Desenvolvimento

### 1. Adicionar Nova Funcionalidade

```bash
# 1. Criar branch
git checkout -b feature/nova-funcionalidade

# 2. Implementar mudanças
# 3. Testar localmente
mvn test

# 4. Commit
git add .
git commit -m "feat: descrição da funcionalidade"

# 5. Push e criar Pull Request
git push origin feature/nova-funcionalidade
```

### 2. Adicionar Novo Endpoint

Exemplo: Adicionar endpoint para listar documentos por tribunal

```java
// 1. Adicionar método no controller
@GetMapping("/documents/{tribunal}")
public ResponseEntity<List<LegalDocument>> getDocumentsByTribunal(
    @PathVariable String tribunal) {
    List<LegalDocument> documents = documentRepository.findByTribunal(tribunal);
    return ResponseEntity.ok(documents);
}

// 2. Testar com curl
curl http://localhost:8080/api/rag/documents/STF

// 3. Adicionar documentação em API_EXAMPLES.md
```

### 3. Adicionar Nova Query Customizada

Exemplo: Buscar documentos por múltiplos critérios

```java
// 1. Adicionar método no repository
@Query("SELECT d FROM LegalDocument d WHERE d.tribunal = :tribunal AND d.legalArea = :area AND d.status = 'VIGENTE'")
List<LegalDocument> findByTribunalAndAreaVigente(
    @Param("tribunal") String tribunal,
    @Param("area") String area);

// 2. Usar no service
List<LegalDocument> docs = documentRepository.findByTribunalAndAreaVigente("STF", "PENAL");
```

## Testes

### Executar Testes

```bash
# Todos os testes
mvn test

# Teste específico
mvn test -Dtest=RAGServiceTest

# Com cobertura
mvn test jacoco:report
```

### Exemplo de Teste Unitário

```java
@SpringBootTest
class RAGServiceTest {

    @Autowired
    private RAGService ragService;

    @Test
    void testRagSync() {
        RAGResponse response = ragService.ragSync(
            "Código Penal",
            "STF",
            "PENAL"
        );

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertTrue(response.getSourceCount() > 0);
    }
}
```

## Debugging

### Modo Debug no IntelliJ IDEA

1. Abra `RagLegalApplication.java`
2. Clique em Run → Debug 'RagLegalApplication'
3. Use breakpoints para pausar execução

### Logs

Edite `src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: DEBUG
    com.rag.legal: DEBUG
    org.springframework.web: DEBUG
```

### Verificar Índice BM25

```java
// Adicionar em RAGService para debug
List<Map<String, Object>> bm25Results = bm25SearchService.search(query, 10);
System.out.println("BM25 Results: " + bm25Results);
```

## Performance

### Profiling com JProfiler

```bash
# Executar com profiler
java -agentpath:/path/to/jprofiler/bin/linux-x64/libjprofilerti.so=port=8849 \
  -jar target/rag-legal-app-1.0.0.jar
```

### Benchmarks

```bash
# Usar JMH para benchmarks
mvn jmh:benchmark
```

### Monitorar Memória

```bash
# Ver uso de memória
jps -l
jmap -heap <pid>
```

## Configuração de IDE

### IntelliJ IDEA

1. **Importar projeto**
   - File → Open → Selecionar `pom.xml`
   - Marcar "Open as Project"

2. **Configurar SDK**
   - File → Project Structure → Project
   - Selecionar Java 17

3. **Habilitar Lombok**
   - File → Settings → Plugins
   - Instalar "Lombok"
   - Habilitar annotation processing

### VS Code

1. **Instalar extensões**
   - Extension Pack for Java
   - Spring Boot Extension Pack

2. **Configurar launch.json**
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot App",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "mainClass": "com.rag.legal.RagLegalApplication",
      "projectName": "rag-legal-app",
      "preLaunchTask": "maven: clean",
      "console": "integratedTerminal"
    }
  ]
}
```

## Integração Contínua

### GitHub Actions

Criar `.github/workflows/ci.yml`:

```yaml
name: CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - run: mvn clean install
      - run: mvn test
```

## Boas Práticas

### 1. Código

- Seguir convenção de nomenclatura Java
- Usar Lombok para reduzir boilerplate
- Adicionar Javadoc em métodos públicos
- Manter métodos pequenos e focados

### 2. Commits

```bash
# Bom commit
git commit -m "feat: implementar reranking com LLM"

# Ruim
git commit -m "fix stuff"
```

### 3. Pull Requests

- Descrever mudanças claramente
- Referenciar issues relacionadas
- Adicionar testes
- Atualizar documentação

### 4. Segurança

- Nunca commitar secrets
- Usar variáveis de ambiente
- Validar entrada do usuário
- Sanitizar queries

## Troubleshooting

### Erro: "Cannot find symbol: class LegalDocument"

```bash
# Limpar cache Maven
mvn clean
mvn compile
```

### Erro: "Port 8080 already in use"

```bash
# Encontrar processo usando porta
lsof -i :8080

# Matar processo
kill -9 <PID>

# Ou mudar porta em application.yml
server:
  port: 8081
```

### Erro: "OpenAI API Key not configured"

```bash
# Verificar variável de ambiente
echo $OPENAI_API_KEY

# Se vazio, configurar
export OPENAI_API_KEY=sk-your-key-here
```

### Erro: "No documents found"

```bash
# Verificar dados de inicialização
curl http://localhost:8080/api/rag/search \
  -H "Content-Type: application/json" \
  -d '{"query": "*", "limit": 100}'
```

## Recursos Úteis

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)
- [Apache Lucene Documentation](https://lucene.apache.org/core/9_8_0/)
- [Java 17 Features](https://www.oracle.com/java/technologies/javase/17-relnotes.html)

## Contato

Para dúvidas ou sugestões sobre desenvolvimento, abra uma issue no repositório.

---

Última atualização: 2026-04-04
