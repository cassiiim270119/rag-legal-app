package com.rag.legal.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do OpenAPI / Swagger para a aplicação RAG Legal
 * 
 * Acesse em: http://localhost:8080/swagger-ui.html
 * JSON OpenAPI: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenAPIConfig {

    @Value("${langchain4j.groq.api-key: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}")
    private String groqApiKey;

    @Value("${langchain4j.groq.model-name:llama-3.3-70b-versatile}")
    private String modelName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RAG Legal API")
                .version("1.0.0")
                .description("API de Retrieval-Augmented Generation para documentos jurídicos com busca híbrida, metadados e upload de PDFs")
                .contact(new Contact()
                    .name("RAG Legal Team")
                    .url("https://github.com/cassiiim270119/rag-legal-app")
                    .email("support@rag-legal.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("https://rag-legal-app.onrender.com")
                    .description("Production Server"),
                new Server()
                    .url("http://localhost:8080")
                    .description("Development Server")
            ));
    }

    @Bean
    public ChatLanguageModel customChatModel() {
//        return OpenAiChatModel.builder()
//                .modelName("openai/gpt-oss-20b")
//                .baseUrl("https://api.groq.com/openai/v1")
//                .apiKey("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")
//                .logRequests(true)
//                .logResponses(true)
//                .build();
        return OpenAiChatModel.builder()
                .apiKey(groqApiKey)
                .modelName(modelName)
                .baseUrl("https://api.groq.com/openai/v1")
                .temperature(0.3)
                .topP(0.9)
                .build();
    }
}
