package com.rag.legal.service;

import com.rag.legal.dto.RAGResponse;
import com.rag.legal.dto.SearchResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;

@Service
@Slf4j
public class RAGService {

    @Autowired
    private HybridSearchService hybridSearchService;

    @Value("${langchain4j.groq.api-key:}")
    private String groqApiKey;

    @Value("${langchain4j.groq.model-name:mixtral-8x7b-32768}")
    private String modelName;

    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingChatModel;

    public RAGService() {
        initializeModels();
    }

    private void initializeModels() {
        try {
            // Usar Groq como alternativa gratuita ao OpenAI
            // Groq oferece acesso gratuito a modelos como Mixtral 8x7B
            if (groqApiKey != null && !groqApiKey.isEmpty()) {
                log.info("Inicializando modelos com Groq (API gratuita) - Modelo: {}", modelName);
                
                // Groq é compatível com OpenAI SDK
                this.chatModel = OpenAiChatModel.builder()
                    .apiKey(groqApiKey)
                    .modelName(modelName)
                    .baseUrl("https://api.groq.com/openai/v1")
                    .temperature(0.3)
                    .topP(0.9)
                    .build();

                this.streamingChatModel = OpenAiStreamingChatModel.builder()
                    .apiKey(groqApiKey)
                    .modelName(modelName)
                    .baseUrl("https://api.groq.com/openai/v1")
                    .temperature(0.3)
                    .topP(0.9)
                    .build();
                    
                log.info("Modelos Groq inicializados com sucesso");
            } else {
                log.warn("GROQ_API_KEY não configurada. Usando modo simulado sem LLM.");
                log.warn("Para usar LLM, configure a variável de ambiente GROQ_API_KEY");
                log.warn("Obtenha uma API Key gratuita em: https://console.groq.com");
            }
        } catch (Exception e) {
            log.warn("Erro ao inicializar modelos Groq: {}. Usando modo simulado.", e.getMessage());
        }
    }

    /**
     * Executa RAG síncrono: busca híbrida + geração com LLM
     */
    public RAGResponse ragSync(String query, String tribunal, String legalArea) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Iniciando RAG síncrono para query: {}", query);

            // 1. Busca híbrida
            List<SearchResult> sources = hybridSearchService.hybridSearch(query, tribunal, legalArea, 10);

            if (sources.isEmpty()) {
                log.warn("Nenhum documento encontrado para a query: {}", query);
                return RAGResponse.builder()
                    .query(query)
                    .answer("Desculpe, não encontrei documentos jurídicos relevantes para sua consulta.")
                    .sources(sources)
                    .sourceCount(0)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            }

            // 2. Construir contexto com fontes
            String context = buildContext(sources);

            // 3. Gerar resposta com LLM
            String answer = generateAnswer(query, context);

            log.info("RAG síncrono concluído em {} ms", System.currentTimeMillis() - startTime);

            return RAGResponse.builder()
                .query(query)
                .answer(answer)
                .sources(sources)
                .sourceCount(sources.size())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();

        } catch (Exception e) {
            log.error("Erro no RAG síncrono", e);
            return RAGResponse.builder()
                .query(query)
                .answer("Erro ao processar sua consulta: " + e.getMessage())
                .sourceCount(0)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }

    /**
     * Executa RAG com streaming: busca híbrida + geração com stream
     */
    public Flux<String> ragStream(String query, String tribunal, String legalArea) {
        return Flux.create(sink -> {
            try {
                log.info("Iniciando RAG com stream para query: {}", query);

                // 1. Busca híbrida
                List<SearchResult> sources = hybridSearchService.hybridSearch(query, tribunal, legalArea, 10);

                if (sources.isEmpty()) {
                    sink.next("Desculpe, não encontrei documentos jurídicos relevantes para sua consulta.");
                    sink.complete();
                    return;
                }

                // 2. Construir contexto
                String context = buildContext(sources);

                // 3. Gerar resposta com stream
                generateAnswerStream(query, context, sink);

            } catch (Exception e) {
                log.error("Erro no RAG com stream", e);
                sink.error(new RuntimeException("Erro ao processar sua consulta: " + e.getMessage()));
            }
        });
    }

    /**
     * Constrói contexto a partir dos documentos encontrados
     */
    private String buildContext(List<SearchResult> sources) {
        StringBuilder context = new StringBuilder();
        context.append("DOCUMENTOS JURÍDICOS RELEVANTES:\\n\\n");

        for (int i = 0; i < sources.size(); i++) {
            SearchResult source = sources.get(i);
            context.append(String.format("Documento %d:\\n", i + 1));
            context.append(String.format("Número: %s\\n", source.getDocumentNumber()));
            context.append(String.format("Título: %s\\n", source.getTitle()));
            context.append(String.format("Tribunal: %s\\n", source.getTribunal()));
            context.append(String.format("Área: %s\\n", source.getLegalArea()));
            context.append(String.format("Status: %s\\n", source.getStatus()));
            context.append(String.format("Conteúdo: %s\\n\\n", source.getContent().substring(0, Math.min(500, source.getContent().length()))));
        }

        return context.toString();
    }

    /**
     * Gera resposta usando LLM (síncrono)
     */
    private String generateAnswer(String query, String context) {
        try {
            String prompt = String.format(
                "Você é um assistente jurídico especializado. Com base nos documentos fornecidos, responda à seguinte pergunta:\\n\\n" +
                "Pergunta: %s\\n\\n" +
                "%s\\n\\n" +
                "Responda de forma clara, precisa e cite os documentos utilizados.",
                query, context
            );

            if (chatModel != null) {
                var response = chatModel.generate(prompt);
                return response.content().text();
            } else {
                // Fallback: resposta simulada
                return "Resposta simulada baseada na consulta: " + query + 
                       "\n\nNota: Para usar LLM com Groq, configure a variável GROQ_API_KEY";
            }
        } catch (Exception e) {
            log.error("Erro ao gerar resposta com LLM", e);
            return "Erro ao gerar resposta: " + e.getMessage();
        }
    }

    /**
     * Gera resposta com streaming
     */
    private void generateAnswerStream(String query, String context, FluxSink<String> sink) {
        try {
            String prompt = String.format(
                "Você é um assistente jurídico especializado. Com base nos documentos fornecidos, responda à seguinte pergunta:\\n\\n" +
                "Pergunta: %s\\n\\n" +
                "%s\\n\\n" +
                "Responda de forma clara, precisa e cite os documentos utilizados.",
                query, context
            );

            if (streamingChatModel != null) {
                streamingChatModel.generate(prompt, new dev.langchain4j.model.chat.StreamingChatLanguageModel.StreamingChatModelCallback() {
                    @Override
                    public void onNext(String token) {
                        sink.next(token);
                    }

                    @Override
                    public void onComplete(dev.langchain4j.model.chat.response.ChatResponse response) {
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        sink.error(error);
                    }
                });
            } else {
                // Fallback: resposta simulada
                sink.next("Resposta simulada com stream para: " + query);
                sink.next("\n\nNota: Para usar LLM com Groq, configure a variável GROQ_API_KEY");
                sink.complete();
            }
        } catch (Exception e) {
            log.error("Erro ao gerar resposta com stream", e);
            sink.error(e);
        }
    }
}
