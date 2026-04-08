package com.rag.legal.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Serviço para extração de texto e metadados de PDFs
 * Utiliza Apache PDFBox 2.0.29 para processamento de PDFs
 */
@Service
@Slf4j
public class PDFExtractionService {

    /**
     * Extrai texto completo de um PDF
     */
    public String extractTextFromPDF(MultipartFile file) throws IOException {
        try {
            PDDocument document = PDDocument.load(file.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            
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
        try {
            PDDocument document = PDDocument.load(file.getInputStream());
            
            metadata.put("fileName", file.getOriginalFilename());
            metadata.put("fileSize", String.valueOf(file.getSize()));
            metadata.put("contentType", file.getContentType());
            metadata.put("pageCount", String.valueOf(document.getNumberOfPages()));
            
            // Tenta extrair informações do documento
            var docInfo = document.getDocumentInformation();
            if (docInfo != null) {
                if (docInfo.getTitle() != null) metadata.put("title", docInfo.getTitle());
                if (docInfo.getAuthor() != null) metadata.put("author", docInfo.getAuthor());
                if (docInfo.getSubject() != null) metadata.put("subject", docInfo.getSubject());
                if (docInfo.getCreator() != null) metadata.put("creator", docInfo.getCreator());
            }
            
            document.close();
            log.info("Metadados extraídos: {}", metadata);
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
        try {
            PDDocument document = PDDocument.load(file.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                pages.add(pageText);
            }
            
            document.close();
            log.info("Texto extraído por página: {} páginas", pages.size());
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

    public List<String> chunkBySize(final String text) {
        Document document = Document.from(text);
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 75);
        return splitter.split(document)
                .stream()
                .map(TextSegment::text)
                .toList();
    }

    /**
     * Divide texto em chunks por parágrafo
     */
    public List<String> chunkTextByParagraph(String text, int minChunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > minChunkSize && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph).append("\n\n");
        }
        
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }
        
        log.info("Texto dividido em {} chunks", chunks.size());
        return chunks;
    }
}
