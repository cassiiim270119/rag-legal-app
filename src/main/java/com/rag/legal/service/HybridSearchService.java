package com.rag.legal.service;

import com.rag.legal.domain.LegalDocument;
import com.rag.legal.dto.SearchResult;
import com.rag.legal.repository.LegalDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HybridSearchService {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private BM25SearchService bm25SearchService;

    @Autowired
    private LegalDocumentRepository documentRepository;

    @Value("${rag.hybrid-search.bm25-weight:0.4}")
    private double bm25Weight;

    @Value("${rag.hybrid-search.vector-weight:0.6}")
    private double vectorWeight;

    @Value("${rag.reranking.enabled:true}")
    private boolean rerankingEnabled;

    @Value("${rag.reranking.top-k:5}")
    private int rerankingTopK;

    /**
     * Executa busca híbrida combinando BM25 e busca vetorial
     */
    public List<SearchResult> hybridSearch(String query, String tribunal, String legalArea, int limit) {
        try {
            log.info("Iniciando busca híbrida: query={}, tribunal={}, legalArea={}", query, tribunal, legalArea);

            // 1. Busca BM25
            List<Map<String, Object>> bm25Results = bm25SearchService.searchVigente(query, limit * 2);
            Map<Long, Double> bm25Scores = normalizeBM25Scores(bm25Results);

            // 2. Busca Vetorial
            float[] queryEmbedding = embeddingService.embed(query);
            List<LegalDocument> allDocuments = documentRepository.findByStatus("VIGENTE");
            Map<Long, Double> vectorScores = calculateVectorScores(allDocuments, queryEmbedding);

            // 3. Aplicar filtros de metadados
            if (tribunal != null && !tribunal.isEmpty()) {
                allDocuments = allDocuments.stream()
                    .filter(d -> d.getTribunal().equals(tribunal))
                    .collect(Collectors.toList());
            }
            if (legalArea != null && !legalArea.isEmpty()) {
                allDocuments = allDocuments.stream()
                    .filter(d -> d.getLegalArea().equals(legalArea))
                    .collect(Collectors.toList());
            }

            // 4. Combinar scores (Hybrid Fusion)
            Map<Long, Double> hybridScores = new HashMap<>();
            Set<Long> allDocIds = new HashSet<>();
            allDocIds.addAll(bm25Scores.keySet());
            allDocIds.addAll(vectorScores.keySet());

            for (Long docId : allDocIds) {
                double bm25Score = bm25Scores.getOrDefault(docId, 0.0);
                double vectorScore = vectorScores.getOrDefault(docId, 0.0);
                double hybridScore = (bm25Weight * bm25Score) + (vectorWeight * vectorScore);
                hybridScores.put(docId, hybridScore);
            }

            // 5. Ordenar por score híbrido
            List<SearchResult> results = hybridScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(entry -> {
                    LegalDocument doc = documentRepository.findById(entry.getKey()).orElse(null);
                    return doc != null ? mapToSearchResult(doc, entry.getValue()) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 6. Aplicar Reranking se habilitado
            if (rerankingEnabled && results.size() > rerankingTopK) {
                results = rerankResults(results, query);
            }

            log.info("Busca híbrida retornou {} resultados", results.size());
            return results;

        } catch (Exception e) {
            log.error("Erro na busca híbrida", e);
            throw new RuntimeException("Falha na busca híbrida", e);
        }
    }

    /**
     * Normaliza scores BM25 para intervalo [0, 1]
     */
    private Map<Long, Double> normalizeBM25Scores(List<Map<String, Object>> bm25Results) {
        Map<Long, Double> scores = new HashMap<>();

        if (bm25Results.isEmpty()) {
            return scores;
        }

        double maxScore = bm25Results.stream()
            .mapToDouble(r -> ((Number) r.get("bm25Score")).doubleValue())
            .max()
            .orElse(1.0);

        for (Map<String, Object> result : bm25Results) {
            Long docId = Long.parseLong((String) result.get("id"));
            double normalizedScore = ((Number) result.get("bm25Score")).doubleValue() / maxScore;
            scores.put(docId, normalizedScore);
        }

        return scores;
    }

    /**
     * Calcula scores de similaridade vetorial
     */
    private Map<Long, Double> calculateVectorScores(List<LegalDocument> documents, float[] queryEmbedding) {
        Map<Long, Double> scores = new HashMap<>();

        for (LegalDocument doc : documents) {
            if (doc.getEmbedding() != null) {
                double similarity = embeddingService.cosineSimilarity(queryEmbedding, doc.getEmbedding());
                // Normalizar para [0, 1] (cosine similarity já está nesse intervalo)
                scores.put(doc.getId(), similarity);
            }
        }

        return scores;
    }

    /**
     * Aplica reranking aos resultados usando LLM (simulado com heurística)
     */
    private List<SearchResult> rerankResults(List<SearchResult> results, String query) {
        log.info("Aplicando reranking aos {} resultados", results.size());

        // Simulação de reranking: priorizar documentos com maior relevância textual
        List<SearchResult> reranked = results.stream()
            .peek(result -> {
                // Score de reranking baseado em relevância textual
                String content = result.getContent().toLowerCase();
                String queryLower = query.toLowerCase();
                int matches = countOccurrences(content, queryLower);
                double rerankScore = result.getScore() * (1.0 + (matches * 0.1));
                result.setRerankScore(rerankScore);
            })
            .sorted((a, b) -> Double.compare(b.getRerankScore(), a.getRerankScore()))
            .limit(rerankingTopK)
            .collect(Collectors.toList());

        return reranked;
    }

    /**
     * Conta ocorrências de uma substring em uma string
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * Mapeia LegalDocument para SearchResult
     */
    private SearchResult mapToSearchResult(LegalDocument doc, double score) {
        return SearchResult.builder()
            .id(doc.getId())
            .documentNumber(doc.getDocumentNumber())
            .title(doc.getTitle())
            .content(doc.getContent())
            .tribunal(doc.getTribunal())
            .legalArea(doc.getLegalArea())
            .status(doc.getStatus())
            .documentType(doc.getDocumentType())
            .publicationDate(doc.getPublicationDate().toString())
            .score(score)
            .metadata(doc.getMetadata())
            .build();
    }
}
