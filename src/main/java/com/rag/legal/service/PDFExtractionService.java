package com.rag.legal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Serviço para extração de texto e metadados de PDFs
 * Implementação simplificada que processa o arquivo como texto
 */
@Service
@Slf4j
public class PDFExtractionService {

    /**
     * Extrai texto completo de um PDF
     * Nota: Esta é uma implementação simplificada que trata o arquivo como texto
     * Para produção, considere usar uma biblioteca como Apache PDFBox ou iText
     */
    public String extractTextFromPDF(MultipartFile file) throws IOException {
        try {
            String text = new String(file.getBytes());
            log.info("Texto extraído do arquivo: {} caracteres", text.length());
            return text;
        } catch (IOException e) {
            log.error("Erro ao extrair texto do arquivo: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Extrai metadados do arquivo
     */
    public Map<String, String> extractMetadata(MultipartFile file) throws IOException {
        Map<String, String> metadata = new HashMap<>();
        try {
            metadata.put("fileName", file.getOriginalFilename());
            metadata.put("fileSize", String.valueOf(file.getSize()));
            metadata.put("contentType", file.getContentType());
            metadata.put("characterCount", String.valueOf(file.getBytes().length));
            
            log.info("Metadados extraídos: {}", metadata);
            return metadata;
        } catch (IOException e) {
            log.error("Erro ao extrair metadados do arquivo: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Extrai texto por página (simula divisão por linhas)
     */
    public List<String> extractTextByPage(MultipartFile file) throws IOException {
        List<String> pages = new ArrayList<>();
        try {
            String text = new String(file.getBytes());
            String[] lines = text.split("\\n");
            
            // Agrupa linhas em "páginas" (100 linhas por página)
            StringBuilder currentPage = new StringBuilder();
            int lineCount = 0;
            for (String line : lines) {
                currentPage.append(line).append("\n");
                lineCount++;
                if (lineCount >= 100) {
                    pages.add(currentPage.toString());
                    currentPage = new StringBuilder();
                    lineCount = 0;
                }
            }
            if (currentPage.length() > 0) {
                pages.add(currentPage.toString());
            }
            
            log.info("Texto dividido em {} páginas", pages.size());
            return pages;
        } catch (IOException e) {
            log.error("Erro ao extrair texto por página: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Limpa e normaliza texto extraído
     */
    public String cleanText(String text) {
        return text
            .replaceAll("\\s+", " ")
            .replaceAll("[^\\w\\s\\-.,;:()\\[\\]{}]", "")
            .trim();
    }

    /**
     * Divide texto em chunks por parágrafo
     */
    public List<String> chunkTextByParagraph(String text, int minChunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > minChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph).append("\n\n");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        log.info("Texto dividido em {} chunks", chunks.size());
        return chunks;
    }
}
