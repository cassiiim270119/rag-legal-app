package com.rag.legal.controller;

import com.rag.legal.dto.RAGResponse;
import com.rag.legal.dto.SearchRequest;
import com.rag.legal.dto.SearchResult;
import com.rag.legal.service.HybridSearchService;
import com.rag.legal.service.RAGService;
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
public class RAGController {

    @Autowired
    private RAGService ragService;

    @Autowired
    private HybridSearchService hybridSearchService;

    /**
     * Endpoint síncrono para RAG
     * POST /api/rag/query
     * Body: { "query": "...", "tribunal": "STF", "legalArea": "CIVIL" }
     */
    @PostMapping("/query")
    public ResponseEntity<RAGResponse> querySync(@RequestBody SearchRequest request) {
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
    public Flux<String> queryStream(@RequestBody SearchRequest request) {
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
     * Endpoint para busca híbrida (sem geração com LLM)
     * POST /api/rag/search
     * Body: { "query": "...", "tribunal": "STF", "legalArea": "CIVIL", "limit": 10 }
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestBody SearchRequest request) {
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
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RAG Service is running");
    }
}
