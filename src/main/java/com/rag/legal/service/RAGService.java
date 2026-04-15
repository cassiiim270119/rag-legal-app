package com.rag.legal.service;

import com.rag.legal.dto.RAGResponse;
import com.rag.legal.dto.SearchResult;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RAGService {

    private static final List<String> OUT_OF_SCOPE_KEYWORDS = Arrays.asList(
            "previsão do tempo", "clima", "futebol", "receita de", "como cozinhar",
            "filme", "série", "música", "jogo", "esporte", "novela",
            "horóscopo", "signo", "loteria", "mega sena",
            "programação", "python", "javascript", "código", "software",
            "piada", "conte uma história", "era uma vez",
            "quem é você", "quem te criou", "qual seu nome verdadeiro");

    private static final List<String> SCOPE_KEYWORDS = Arrays.asList(
            "contrato", "clt", "trabalhista", "demissão", "rescisão", "férias",
            "fgts", "justa causa", "aviso prévio", "hora extra", "salário",
            "cobrança", "inadimplente", "protesto", "execução", "dívida",
            "lgpd", "dados pessoais", "consentimento", "proteção de dados",
            "consumidor", "cdc", "garantia", "devolução", "recall",
            "aluguel", "inquilino", "locação", "despejo", "renovatória",
            "empresa", "cnpj", "sócio", "sociedade", "mei", "ltda",
            "lei", "artigo", "súmula", "jurídico", "advogado", "direito",
            "indenização", "dano moral", "responsabilidade", "obrigação",
            "nota fiscal", "fornecedor", "licitação", "compliance");

    private static final List<String> GREETINGS = Arrays.asList("olá", "oi", "bom dia", "boa tarde", "boa noite",
            "hello", "hi", "obrigado", "obrigada", "valeu", "tchau");

    private static final String GREETING_AJI =  """
                Olá! 😊 Eu sou o AJI, minha especialidade é **orientação jurídica para empresas brasileiras**.

                Posso ajudar com questões sobre:

                - 📋 **Contratos** empresariais

                - 👷 **Direito do Trabalho** (CLT, rescisão, férias)

                - 💰 **Cobrança** de inadimplentes

                - 🔒 **LGPD** e proteção de dados

                - 🛒 **Direito do Consumidor**

                - 🏢 **Direito Empresarial** em geral


                Como posso ajudá-lo nessa área?
            """;

    private static final String REJECTION_AJI =  """
                Agradeço sua pergunta! 😊
                No entanto, minha especialidade é **orientação jurídica para empresas brasileiras**.

                Posso ajudar com questões sobre:

                - 📋 **Contratos** empresariais

                - 👷 **Direito do Trabalho** (CLT, rescisão, férias)

                - 💰 **Cobrança** de inadimplentes

                - 🔒 **LGPD** e proteção de dados

                - 🛒 **Direito do Consumidor**

                - 🏢 **Direito Empresarial** em geral


                Como posso ajudá-lo nessa área?
            """;

    private final HybridSearchService hybridSearchService;
    private final ChatModel chatModel;

    /**
     * Executa RAG síncrono: busca híbrida + geração com LLM
     */
    public RAGResponse ragSync(final String query, final String tribunal, final String legalArea) {
        long startTime = System.currentTimeMillis();

        if (isGreeting(query)) {
            return RAGResponse.builder()
                    .query(query)
                    .answer(GREETING_AJI)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        if (isOutOfScope(query)) {
            return RAGResponse.builder()
                    .query(query)
                    .answer(REJECTION_AJI)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

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
     * Implementação simplificada que envia a resposta em chunks
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

                // 3. Gerar resposta e enviar em chunks
                String answer = generateAnswer(query, context);
                
                // Dividir resposta em chunks de 100 caracteres para simular streaming
                int chunkSize = 100;
                for (int i = 0; i < answer.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, answer.length());
                    String chunk = answer.substring(i, end);
                    sink.next(chunk);
                    
                    // Pequeno delay para simular streaming
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                sink.complete();

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

        for (int i = 0; i < sources.size(); i++) {
            SearchResult source = sources.get(i);
            context.append(String.format("Documento %d:\n", i + 1));
            context.append(String.format("Número: %s\n", source.getDocumentNumber()));
            context.append(String.format("Título: %s\n", source.getTitle()));
            context.append(String.format("Área: %s\n", source.getLegalArea()));
            context.append(String.format("Status: %s\n", source.getStatus()));
            context.append(String.format("Conteúdo: %s\n\n", source.getContent()));
        }

        return context.toString();
    }

    /**
     * Gera resposta usando LLM (síncrono)
     */
    private String generateAnswer(String query, String context) {
        try {
            List<ChatMessage> messages = Arrays.asList(new SystemMessage(getPrompt()), new SystemMessage(context),
                    new UserMessage(query));

            if (chatModel != null) {
                return chatModel.chat(messages).aiMessage().text();
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

    private boolean isGreeting(final String query) {
        if (StringUtils.isEmpty(query)){
            return false;
        }
        final String query_lower = query.toLowerCase();
        return GREETINGS.stream().anyMatch(kw -> query_lower.equalsIgnoreCase(kw) || query_lower.startsWith(kw));
    }

    private boolean isOutOfScope(final String query) {
        if (StringUtils.isEmpty(query)){
            return false;
        }
        final String query_lower = query.toLowerCase();
        return OUT_OF_SCOPE_KEYWORDS.stream().anyMatch(query_lower::contains);
    }

    private String getPrompt() {
        return """
            Você é o **AJI — Assistente Jurídico Inteligente**, um assistente virtual especializado em orientação jurídica para empresários brasileiros de pequenas e médias empresas.
            
            ## Sua Identidade
            
            - Nome: AJI (Assistente Jurídico Inteligente)
            - Tom: Profissional, acessível e empático. Você fala como um consultor de confiança, não como um robô.
            - Idioma: Sempre em português brasileiro.
            
            ## Seu Escopo de Atuação
            
            Você PODE ajudar com:
            - Direito do Trabalho (CLT, rescisão, férias, FGTS, justa causa, etc.)
            - Contratos Empresariais (elaboração, revisão, cláusulas, rescisão)
            - Cobrança de Inadimplentes (protesto, execução, negociação)
            - LGPD (proteção de dados, adequação, consentimento)
            - Direito do Consumidor (CDC, devoluções, garantias, recalls)
            - Lei do Inquilinato (aluguel comercial, despejo, renovatória)
            - Direito Empresarial geral (abertura, fechamento, tipos societários)
            
            Você NÃO PODE ajudar com:
            - Qualquer assunto que não seja jurídico empresarial brasileiro
            - Direito Penal, Direito de Família, Direito Tributário complexo
            - Previsão do tempo, receitas, esportes, entretenimento, tecnologia geral
            - Qualquer tema fora do escopo jurídico empresarial
            
            ## Regras Inegociáveis
            
            1. **DISCLAIMER OBRIGATÓRIO:** Toda resposta substantiva DEVE terminar com:
               > ⚠️ *Esta orientação tem caráter informativo e educacional. Não substitui a consulta a um advogado. Para decisões jurídicas concretas, consulte um profissional habilitado pela OAB.*
            
            2. **NUNCA diga que é advogado.** Você é um assistente de orientação.
            
            3. **NUNCA recomende ações judiciais específicas.** Você orienta, não litiga.
            
            4. **Se não souber a resposta:** Diga honestamente que não tem informação suficiente e sugira consultar um advogado especialista.
            
            5. **Rejeição educada:** Se o usuário perguntar algo fora do escopo, responda com cordialidade:
               "Agradeço sua pergunta! No entanto, minha especialidade é orientação jurídica para empresas brasileiras. Posso ajudar com questões sobre contratos, direito do trabalho, cobrança, LGPD e outros temas jurídicos empresariais. Como posso ajudá-lo nessa área?"
            
            6. **Sem bajulação:** Seja técnico e honesto. Se uma situação é arriscada para o empresário, diga claramente.
            
            7. **Cite a legislação:** Sempre que possível, mencione o artigo de lei, súmula ou enunciado relevante.
            
            ## Como Usar o Contexto
            
            Você receberá trechos relevantes da legislação brasileira como contexto. Use-os para embasar suas respostas com citações específicas. Se o contexto não contiver informação relevante, use seu conhecimento geral jurídico, mas informe que a resposta é baseada em conhecimento geral.
            
            ## Formato de Resposta
            
            - Use linguagem clara e acessível (o usuário é empresário, não advogado)
            - Estruture com tópicos quando a resposta for longa
            - Use **negrito** para termos jurídicos importantes
            - Inclua exemplos práticos quando relevante
            - Mantenha respostas concisas (máximo 500 palavras, exceto quando a complexidade exigir mais)
            """;
    }
}
