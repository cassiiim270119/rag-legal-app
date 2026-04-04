package com.rag.legal.service;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    @Value("${rag.embedding.dimension:384}")
    private int embeddingDimension;

    public EmbeddingService() {
        // Usando modelo local all-minilm-l6-v2 para evitar custos de API
        // Este modelo é incluído na dependência langchain4j-embeddings-all-minilm-l6-v2
        try {
            this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
            log.info("EmbeddingService inicializado com modelo local all-minilm-l6-v2 (384 dimensões)");
        } catch (Exception e) {
            log.error("Erro ao inicializar EmbeddingModel", e);
            throw new RuntimeException("Falha ao inicializar EmbeddingService", e);
        }
    }

    /**
     * Gera embedding para um texto único
     */
    public float[] embed(String text) {
        try {
            var embedding = embeddingModel.embed(text);
            return embedding.vector();
        } catch (Exception e) {
            log.error("Erro ao gerar embedding para texto: {}", text.substring(0, Math.min(50, text.length())), e);
            throw new RuntimeException("Falha ao gerar embedding", e);
        }
    }

    /**
     * Gera embeddings para múltiplos textos
     */
    public List<float[]> embedBatch(List<String> texts) {
        try {
            var embeddings = embeddingModel.embedAll(texts);
            return embeddings.embeddings().stream()
                .map(e -> e.vector())
                .toList();
        } catch (Exception e) {
            log.error("Erro ao gerar embeddings em batch", e);
            throw new RuntimeException("Falha ao gerar embeddings em batch", e);
        }
    }

    /**
     * Retorna a dimensão do embedding
     */
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
}
