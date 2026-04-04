package com.rag.legal.service;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
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
        this.embeddingModel = new OnnxEmbeddingModel(
            "src/main/resources/models/all-minilm-l6-v2"
        );
        log.info("EmbeddingService inicializado com modelo local all-minilm-l6-v2");
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
            log.error("Erro ao gerar embeddings em batch para {} textos", texts.size(), e);
            throw new RuntimeException("Falha ao gerar embeddings em batch", e);
        }
    }

    /**
     * Calcula similaridade de cosseno entre dois vetores
     */
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vetores devem ter o mesmo tamanho");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Retorna a dimensão dos embeddings
     */
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
}
