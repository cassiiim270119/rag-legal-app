package com.rag.legal.controller;

import com.rag.legal.dto.*;
import com.rag.legal.service.HybridSearchService;
import com.rag.legal.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/chat")
@Slf4j
@Tag(name = "RAG Endpoints", description = "Endpoints para Retrieval-Augmented Generation com busca híbrida")
public class ChatController {

    @Autowired
    private RAGService ragService;

    @Autowired
    private HybridSearchService hybridSearchService;

    /**
     * Endpoint síncrono para Chat
     * POST /api/chat
     * Body: { "message": "Texto da mensagem" }
     */
    @PostMapping
    @Operation(summary = "Q&A AJI", description = "Recebe pergunta e responde de firma síncrona")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<ChatResponse> querySync(
            @RequestBody @Parameter(description = "Pergunta do usuário") final Map<String, String> request) {
        try {
            log.info("Recebida pregunta de usuário: {}", request);

            if (request == null || StringUtils.isEmpty(request.get("message"))) {
                return ResponseEntity.badRequest().build();
            }

            RAGResponse response = ragService.ragSync(request.get("message"), null, null);

            ChatResponse chatResponse = ChatResponse.builder()
                    .response(response.getAnswer())
                    .sources(response.getSources().stream()
                            .map(source -> SourceResponse.builder()
                                    .category("Geral")
                                    .source(source.getTitle())
                                    .relevance("1.0")
                                    .build())
                            .toList())
                    .in_scope("Escopo")
                    .build();
            return ResponseEntity.ok(chatResponse);
        } catch (Exception e) {
            log.error("Erro no endpoint RAG síncrono", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check
     * GET /api/chat/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Verifica se o chat está ativo")
    @ApiResponse(responseCode = "200", description = "Chat está ativo")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AJI chat is running");
    }
}
