package com.rag.legal.controller;

import com.rag.legal.service.PDFIndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para upload e indexação de documentos PDF
 * 
 * Endpoints:
 * - POST /api/pdf/upload - Upload de um único PDF
 * - POST /api/pdf/upload-batch - Upload de múltiplos PDFs
 * - GET /api/pdf/stats - Estatísticas de indexação
 */
@RestController
@RequestMapping("/api/pdf")
@Slf4j
public class PDFUploadController {

    @Autowired
    private PDFIndexingService pdfIndexingService;

    /**
     * Upload e indexação de um único PDF
     * 
     * Exemplo:
     * curl -X POST http://localhost:8080/api/pdf/upload \
     *   -F "file=@documento.pdf" \
     *   -F "tribunal=STF" \
     *   -F "legalArea=PENAL" \
     *   -F "documentType=SUMULA"
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadPDF(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tribunal", required = false) String tribunal,
            @RequestParam(value = "legalArea", required = false) String legalArea,
            @RequestParam(value = "documentType", required = false) String documentType) {

        log.info("Recebido upload de PDF: {}", file.getOriginalFilename());

        // Validar arquivo
        if (file.isEmpty()) {
            log.warn("Arquivo vazio enviado");
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Arquivo vazio"));
        }

        // Validar tipo de arquivo
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            log.warn("Tipo de arquivo inválido: {}", contentType);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Apenas arquivos PDF são aceitos"));
        }

        // Validar tamanho (máximo 50MB)
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            log.warn("Arquivo muito grande: {} bytes", file.getSize());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Arquivo muito grande (máximo 50MB)"));
        }

        try {
            // Processar e indexar PDF
            Map<String, Object> result = pdfIndexingService.indexPDF(
                file, tribunal, legalArea, documentType
            );

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

        } catch (Exception e) {
            log.error("Erro ao processar PDF: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Erro ao processar PDF: " + e.getMessage()
                ));
        }
    }

    /**
     * Upload e indexação de múltiplos PDFs em batch
     * 
     * Exemplo:
     * curl -X POST http://localhost:8080/api/pdf/upload-batch \
     *   -F "files=@doc1.pdf" \
     *   -F "files=@doc2.pdf" \
     *   -F "tribunal=STF" \
     *   -F "legalArea=PENAL"
     */
    @PostMapping("/upload-batch")
    public ResponseEntity<Map<String, Object>> uploadPDFBatch(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "tribunal", required = false) String tribunal,
            @RequestParam(value = "legalArea", required = false) String legalArea,
            @RequestParam(value = "documentType", required = false) String documentType) {

        log.info("Recebido upload em batch de {} PDFs", files.size());

        // Validar lista de arquivos
        if (files.isEmpty()) {
            log.warn("Lista de arquivos vazia");
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Nenhum arquivo enviado"));
        }

        // Validar cada arquivo
        List<MultipartFile> validFiles = new ArrayList<>();
        Map<String, String> invalidFiles = new HashMap<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                invalidFiles.put(file.getOriginalFilename(), "Arquivo vazio");
                continue;
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                invalidFiles.put(file.getOriginalFilename(), "Tipo de arquivo inválido");
                continue;
            }

            long maxSize = 50 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                invalidFiles.put(file.getOriginalFilename(), "Arquivo muito grande");
                continue;
            }

            validFiles.add(file);
        }

        if (validFiles.isEmpty()) {
            log.warn("Nenhum arquivo válido no batch");
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "Nenhum arquivo válido",
                    "invalidFiles", invalidFiles
                ));
        }

        try {
            // Processar batch
            Map<String, Object> result = pdfIndexingService.indexPDFBatch(
                validFiles, tribunal, legalArea, documentType
            );

            if (invalidFiles.size() > 0) {
                result.put("invalidFiles", invalidFiles);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erro ao processar batch de PDFs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "Erro ao processar batch: " + e.getMessage()
                ));
        }
    }

    /**
     * Obtém estatísticas de indexação
     * 
     * Exemplo:
     * curl http://localhost:8080/api/pdf/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = pdfIndexingService.getIndexingStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erro ao obter estatísticas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao obter estatísticas: " + e.getMessage()));
        }
    }

    /**
     * Health check para o serviço de PDF
     * 
     * Exemplo:
     * curl http://localhost:8080/api/pdf/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "PDF Upload and Indexing Service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
