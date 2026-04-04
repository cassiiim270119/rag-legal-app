package com.rag.legal.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Serviço para extração de texto de documentos PDF
 * 
 * Funcionalidades:
 * - Extração de texto bruto
 * - Extração de metadados (título, autor, etc)
 * - Processamento em chunks (páginas ou parágrafos)
 * - Limpeza e normalização de texto
 */
@Service
@Slf4j
public class PDFExtractionService {

    private static final int DEFAULT_CHUNK_SIZE = 500; // caracteres
    private static final int CHUNK_OVERLAP = 100; // caracteres de sobreposição
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\n+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * Extrai texto completo de um PDF
     */
    public String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.loadPDF(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Texto extraído do PDF: {} caracteres", text.length());
            return text;
        } catch (IOException e) {
            log.error("Erro ao extrair texto do PDF: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Extrai metadados do PDF
     */
    public Map<String, String> extractMetadata(MultipartFile file) throws IOException {
        Map<String, String> metadata = new HashMap<>();
        try (PDDocument document = PDDocument.loadPDF(file.getInputStream())) {
            var docInfo = document.getDocumentInformation();
            
            if (docInfo != null) {
                metadata.put("title", docInfo.getTitle() != null ? docInfo.getTitle() : "");
                metadata.put("author", docInfo.getAuthor() != null ? docInfo.getAuthor() : "");
                metadata.put("subject", docInfo.getSubject() != null ? docInfo.getSubject() : "");
                metadata.put("creator", docInfo.getCreator() != null ? docInfo.getCreator() : "");
                metadata.put("producer", docInfo.getProducer() != null ? docInfo.getProducer() : "");
                metadata.put("creationDate", docInfo.getCreationDate() != null ? docInfo.getCreationDate().toString() : "");
                metadata.put("modificationDate", docInfo.getModificationDate() != null ? docInfo.getModificationDate().toString() : "");
            }
            
            metadata.put("pageCount", String.valueOf(document.getNumberOfPages()));
            metadata.put("fileName", file.getOriginalFilename());
            metadata.put("fileSize", String.valueOf(file.getSize()));
            
            log.info("Metadados extraídos do PDF: {}", metadata);
            return metadata;
        } catch (IOException e) {
            log.error("Erro ao extrair metadados do PDF: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Extrai texto por página
     */
    public List<String> extractTextByPage(MultipartFile file) throws IOException {
        List<String> pages = new ArrayList<>();
        try (PDDocument document = PDDocument.loadPDF(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);
                pages.add(pageText);
            }
            
            log.info("Texto extraído por página: {} páginas", pages.size());
            return pages;
        } catch (IOException e) {
            log.error("Erro ao extrair texto por página do PDF: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    /**
     * Divide texto em chunks com sobreposição
     * Útil para RAG quando o documento é muito grande
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // Normalizar whitespace
        String normalizedText = WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
        
        int start = 0;
        while (start < normalizedText.length()) {
            int end = Math.min(start + chunkSize, normalizedText.length());
            
            // Tentar quebrar em espaço se possível
            if (end < normalizedText.length()) {
                int lastSpace = normalizedText.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            
            String chunk = normalizedText.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            // Avançar com sobreposição
            start = end - overlap;
        }
        
        log.info("Texto dividido em {} chunks (tamanho: {}, sobreposição: {})", 
                 chunks.size(), chunkSize, overlap);
        return chunks;
    }

    /**
     * Divide texto em chunks por parágrafo (mais semântico)
     */
    public List<String> chunkByParagraph(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // Dividir por parágrafos
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            String cleanParagraph = WHITESPACE_PATTERN.matcher(paragraph).replaceAll(" ").trim();
            
            if (cleanParagraph.isEmpty()) {
                continue;
            }
            
            // Se adicionar o parágrafo exceder o limite, salvar o chunk atual
            if (currentChunk.length() + cleanParagraph.length() + 1 > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(cleanParagraph);
        }
        
        // Adicionar último chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        log.info("Texto dividido em {} chunks por parágrafo", chunks.size());
        return chunks;
    }

    /**
     * Limpa e normaliza texto
     */
    public String cleanText(String text) {
        if (text == null) {
            return "";
        }
        
        // Remover caracteres especiais problemáticos
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Normalizar espaços em branco
        text = WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
        
        // Remover espaços em branco no início e fim
        text = text.trim();
        
        return text;
    }

    /**
     * Extrai seções de um texto baseado em padrões (ex: "Artigo 1", "§ 1º")
     */
    public Map<String, String> extractSections(String text) {
        Map<String, String> sections = new LinkedHashMap<>();
        
        if (text == null || text.isEmpty()) {
            return sections;
        }
        
        // Padrões comuns em documentos jurídicos
        Pattern sectionPattern = Pattern.compile(
            "(?:^|\\n)((?:Art(?:igo)?|§|Seção|Capítulo|Título)\\s*\\.?\\s*\\d+[^\\n]*)",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
        );
        
        var matcher = sectionPattern.matcher(text);
        int lastEnd = 0;
        String lastSection = "Preâmbulo";
        
        while (matcher.find()) {
            // Salvar conteúdo da seção anterior
            String sectionContent = text.substring(lastEnd, matcher.start()).trim();
            if (!sectionContent.isEmpty()) {
                sections.put(lastSection, sectionContent);
            }
            
            lastSection = matcher.group(1).trim();
            lastEnd = matcher.start();
        }
        
        // Adicionar última seção
        if (lastEnd < text.length()) {
            String sectionContent = text.substring(lastEnd).trim();
            if (!sectionContent.isEmpty()) {
                sections.put(lastSection, sectionContent);
            }
        }
        
        log.info("Extraídas {} seções do texto", sections.size());
        return sections;
    }
}
