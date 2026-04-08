package com.rag.legal.service;

import com.rag.legal.domain.LegalDocument;
import com.rag.legal.repository.LegalDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * Serviço para indexação de documentos PDF
 * 
 * Funcionalidades:
 * - Upload e processamento de PDFs
 * - Extração de texto e metadados
 * - Chunking inteligente
 * - Geração de embeddings
 * - Indexação em BM25
 * - Persistência no banco de dados
 */
@Service
@Slf4j
public class PDFIndexingService {

    @Autowired
    private PDFExtractionService pdfExtractionService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private BM25SearchService bm25SearchService;

    @Autowired
    private LegalDocumentRepository documentRepository;

    private static final int CHUNK_SIZE = 500; // caracteres
    private static final int CHUNK_OVERLAP = 100;

    /**
     * Processa e indexa um arquivo PDF
     */
    public Map<String, Object> indexPDF(final MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            log.info("Iniciando indexação do PDF: {}", file.getOriginalFilename());

            // 1. Extrair texto e metadados
            String fullText = pdfExtractionService.extractTextFromPDF(file);
            Map<String, String> metadata = pdfExtractionService.extractMetadata(file);
            
            // 2. Limpar texto
            String cleanedText = pdfExtractionService.cleanText(fullText);
            
            // 3. Dividir em chunks
            List<String> chunks = pdfExtractionService.chunkTextByParagraph(cleanedText, CHUNK_SIZE);
            
            if (chunks.isEmpty()) {
                log.warn("Nenhum chunk gerado para o PDF: {}", file.getOriginalFilename());
                result.put("success", false);
                result.put("message", "Nenhum conteúdo extraído do PDF");
                return result;
            }

            // 4. Processar cada chunk
            List<LegalDocument> savedDocuments = new ArrayList<>();
            int chunkNumber = 1;
            
            for (String chunk : chunks) {
                if (chunk.length() < 50) {
                    // Ignorar chunks muito pequenos
                    continue;
                }

                try {
                    // Gerar título baseado no primeiro parágrafo
                    String title = generateTitle(metadata.get("title"), chunk, chunkNumber);
                    
                    // Gerar embedding
                    float[] embedding = embeddingService.embed(chunk);
                    
                    // Criar documento
                    LegalDocument document = LegalDocument.builder()
                        .documentNumber(generateDocumentNumber(Objects.requireNonNull(file.getOriginalFilename()), chunkNumber))
                        .documentType("PDF")
                        .title(title)
                        .content(chunk)
                        .tribunal("GERAL")
                        .legalArea("DIVERSOS")
                        .status("VIGENTE")
                        .embedding(embedding)
                        .chapter(file.getOriginalFilename())
                        .paragraph("Chunk " + chunkNumber)
                        .publicationDate(LocalDate.now())
                        .build();
                    
                    // Salvar no banco
                    LegalDocument saved = documentRepository.save(document);
                    savedDocuments.add(saved);
                    
                    // Indexar em BM25
                    bm25SearchService.indexDocument(saved);
                    
                    log.debug("Chunk {} indexado com sucesso", chunkNumber);
                    chunkNumber++;
                    
                } catch (Exception e) {
                    log.error("Erro ao processar chunk {}", chunkNumber, e);
                }
            }

            // 5. Preparar resultado
            result.put("success", true);
            result.put("message", "PDF indexado com sucesso");
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("chunksCreated", savedDocuments.size());
            result.put("totalCharacters", cleanedText.length());
            result.put("pageCount", metadata.get("pageCount"));
            result.put("processingTimeMs", System.currentTimeMillis() - startTime);
            result.put("documents", savedDocuments.stream()
                .map(d -> Map.of(
                    "id", d.getId(),
                    "title", d.getTitle(),
                    "paragraph", d.getParagraph()
                ))
                .toList());

            log.info("PDF indexado com sucesso: {} chunks criados em {} ms", 
                     savedDocuments.size(), System.currentTimeMillis() - startTime);

            return result;

        } catch (IOException e) {
            log.error("Erro ao processar PDF: {}", file.getOriginalFilename(), e);
            result.put("success", false);
            result.put("message", "Erro ao processar PDF: " + e.getMessage());
            result.put("processingTimeMs", System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * Processa múltiplos PDFs em batch
     */
    public Map<String, Object> indexPDFBatch(List<MultipartFile> files, String tribunal, String legalArea, String documentType) {
        Map<String, Object> batchResult = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        log.info("Iniciando indexação em batch de {} arquivos", files.size());

        int successCount = 0;
        int errorCount = 0;

        for (MultipartFile file : files) {
            try {
                Map<String, Object> result = indexPDF(file);
                results.add(result);
                
                if ((Boolean) result.get("success")) {
                    successCount++;
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                log.error("Erro ao processar arquivo em batch: {}", file.getOriginalFilename(), e);
                errorCount++;
                results.add(Map.of(
                    "fileName", Objects.requireNonNull(file.getOriginalFilename()),
                    "success", false,
                    "message", "Erro: " + e.getMessage()
                ));
            }
        }

        batchResult.put("totalFiles", files.size());
        batchResult.put("successCount", successCount);
        batchResult.put("errorCount", errorCount);
        batchResult.put("processingTimeMs", System.currentTimeMillis() - startTime);
        batchResult.put("results", results);

        log.info("Batch concluído: {} sucesso, {} erros em {} ms", 
                 successCount, errorCount, System.currentTimeMillis() - startTime);

        return batchResult;
    }

    /**
     * Gera número único para o documento
     */
    private String generateDocumentNumber(String fileName, int chunkNumber) {
        String baseName = fileName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return String.format("PDF-%s-%04d", baseName.substring(0, Math.min(10, baseName.length())), chunkNumber);
    }

    /**
     * Gera título para o chunk
     */
    private String generateTitle(String pdfTitle, String chunk, int chunkNumber) {
        if (pdfTitle != null && !pdfTitle.isEmpty()) {
            return String.format("%s (Parte %d)", pdfTitle, chunkNumber);
        }

        // Usar primeira linha do chunk como título
        String[] lines = chunk.split("\\n");
        String firstLine = lines[0].trim();
        
        if (firstLine.length() > 100) {
            return firstLine.substring(0, 100) + "...";
        }
        
        return firstLine.isEmpty() ? "Documento PDF - Parte " + chunkNumber : firstLine;
    }

    /**
     * Obtém estatísticas de indexação
     */
    public Map<String, Object> getIndexingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalDocuments = documentRepository.count();
        long pdfDocuments = documentRepository.countByDocumentType("PDF");
        
        stats.put("totalDocuments", totalDocuments);
        stats.put("pdfDocuments", pdfDocuments);
        stats.put("indexedPercentage", totalDocuments > 0 ? (pdfDocuments * 100.0 / totalDocuments) : 0);
        
        return stats;
    }
}
