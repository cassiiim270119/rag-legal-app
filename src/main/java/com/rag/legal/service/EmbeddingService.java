package com.rag.legal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de Embeddings - Implementação simplificada
 * 
 * Em produção, você pode integrar com:
 * - OpenAI Embeddings API
 * - Hugging Face Inference API
 * - Cohere Embeddings API
 * - Groq (quando suportar embeddings)
 */
@Service
@Slf4j
public class EmbeddingService {

    @Value("${rag.embedding.dimension:384}")
    private int embeddingDimension;

    public EmbeddingService() {
        log.info("EmbeddingService inicializado (modo simulado - 384 dimensões)");
        log.info("Para usar embeddings reais, integre com OpenAI, Cohere ou Hugging Face");
    }

    /**
     * Gera embedding para um texto único (modo simulado)
     * 
     * Em produção, substitua por chamada a API real:
     * - OpenAI: https://platform.openai.com/docs/guides/embeddings
     * - Cohere: https://docs.cohere.com/reference/embed
     * - Hugging Face: https://huggingface.co/inference-api
     */
    public float[] embed(String text) {
        try {
            // Modo simulado: gera embedding determinístico baseado no hash do texto
            return generateSimulatedEmbedding(text);
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
            return texts.stream()
                .map(this::embed)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erro ao gerar embeddings em batch", e);
            throw new RuntimeException("Falha ao gerar embeddings em batch", e);
        }
    }

    /**
     * Gera embedding simulado determinístico baseado no hash do texto
     * Garante que o mesmo texto sempre gera o mesmo embedding
     */
    private float[] generateSimulatedEmbedding(String text) {
        float[] embedding = new float[embeddingDimension];
        
        // Usar hash do texto como seed para gerar embedding determinístico
        long seed = text.hashCode();
        java.util.Random random = new java.util.Random(seed);
        
        // Gerar valores aleatórios normalizados
        for (int i = 0; i < embeddingDimension; i++) {
            embedding[i] = (float) random.nextGaussian() / 10.0f;
        }
        
        // Normalizar o vetor
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embeddingDimension; i++) {
                embedding[i] /= norm;
            }
        }
        
        return embedding;
    }

    /**
     * Calcula similaridade de cosseno entre dois embeddings
     */
    public double cosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings devem ter a mesma dimensão");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (norm1 * norm2);
    }

    /**
     * Retorna a dimensão do embedding
     */
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
}
