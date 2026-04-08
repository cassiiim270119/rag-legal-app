package com.rag.legal.controller;

import com.rag.legal.dto.RAGResponse;
import com.rag.legal.dto.SearchRequest;
import com.rag.legal.dto.SearchResult;
import com.rag.legal.service.HybridSearchService;
import com.rag.legal.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/rag")
@Slf4j
@Tag(name = "RAG Endpoints", description = "Endpoints para Retrieval-Augmented Generation com busca híbrida")
public class RAGController {

    @Autowired
    private RAGService ragService;

    @Autowired
    private HybridSearchService hybridSearchService;

    /**
     * Endpoint sín crono para RAG
     * POST /api/rag/query
     * Body: { "query": "...", "tribunal": "STF", "legalArea": "CIVIL" }
     */
    @PostMapping("/query")
    @Operation(summary = "Consulta RAG Síncrona", description = "Executa busca híbrida e gera resposta com LLM de forma síncrona")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<RAGResponse> querySync(
            @RequestBody @Parameter(description = "Requisição de busca") SearchRequest request) {
        try {
            log.info("Recebida requisição RAG síncrona: {}", request.getQuery());

            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            RAGResponse response = ragService.ragSync(
                request.getQuery(),
                request.getTribunal(),
                request.getLegalArea()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro no endpoint RAG síncrono", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint com streaming para RAG
     * POST /api/rag/query-stream
     * Body: { "query": "...", "tribunal": "STF", "legalArea": "CIVIL" }
     * Response: text/event-stream
     */
    @PostMapping(value = "/query-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Consulta RAG com Stream", description = "Executa busca híbrida e retorna resposta em stream (text/event-stream)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stream iniciado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public Flux<String> queryStream(
            @RequestBody @Parameter(description = "Requisição de busca") SearchRequest request) {
        try {
            log.info("Recebida requisição RAG com stream: {}", request.getQuery());

            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return Flux.error(new IllegalArgumentException("Query não pode estar vazia"));
            }

            return ragService.ragStream(
                request.getQuery(),
                request.getTribunal(),
                request.getLegalArea()
            );
        } catch (Exception e) {
            log.error("Erro no endpoint RAG com stream", e);
            return Flux.error(e);
        }
    }

    /**
     * Endpoint para busca híbr ida (sem geração com LLM)
     * POST /api/rag/search
     * Body: { "query": "...", "tribunal": "STF", "legalArea": "CIVIL", "limit": 10 }
     */
    @PostMapping("/search")
    @Operation(summary = "Busca Híbr ida", description = "Executa busca híbr ida (BM25 + Vetorial) sem geração de resposta com LLM")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resultados encontrados"),
        @ApiResponse(responseCode = "400", description = "Requisição inválida"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<SearchResult>> search(
            @RequestBody @Parameter(description = "Requisição de busca") SearchRequest request) {
        try {
            log.info("Recebida requisição de busca híbrida: {}", request.getQuery());

            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<SearchResult> results = hybridSearchService.hybridSearch(
                request.getQuery(),
                request.getTribunal(),
                request.getLegalArea(),
                request.getLimitOrDefault()
            );

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Erro no endpoint de busca híbrida", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check
     * GET /api/rag/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Verifica se o serviço RAG está ativo")
    @ApiResponse(responseCode = "200", description = "Serviço está ativo")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RAG Service is running");
    }
}
