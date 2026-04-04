package com.rag.legal.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.text.pdf.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Serviço para extração de texto e metadados de PDFs
 * Utiliza iText 7 para processamento de PDFs
 */
@Service
@Slf4j
public class PDFExtractionService {

    /**
     * Extrai texto completo de um PDF
     */
    public String extractTextFromPDF(MultipartFile file) throws IOException {
        try {
            byte[] bytes = file.getBytes();
            PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(bytes));
            PdfDocument pdfDoc = new PdfDocument(reader);
            
            StringBuilder text = new StringBuilder();
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                text.append(pageText).append("\n");
            }
            
            pdfDoc.close();
            log.info("Texto extraído do PDF: {} caracteres", text.length());
            return text.toString();
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
            byte[] bytes = file.getBytes();
            PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(bytes));
            PdfDocument pdfDoc = new PdfDocument(reader);
            
            // Metadados básicos
            metadata.put("fileName", file.getOriginalFilename());
            metadata.put("fileSize", String.valueOf(file.getSize()));
            metadata.put("pageCount", String.valueOf(pdfDoc.getNumberOfPages()));
            metadata.put("contentType", file.getContentType());
            
            // Tenta extrair informações do documento
            var docInfo = pdfDoc.getDocumentInfo();
            if (docInfo != null) {
                if (docInfo.getTitle() != null) metadata.put("title", docInfo.getTitle());
                if (docInfo.getAuthor() != null) metadata.put("author", docInfo.getAuthor());
                if (docInfo.getSubject() != null) metadata.put("subject", docInfo.getSubject());
                if (docInfo.getCreator() != null) metadata.put("creator", docInfo.getCreator());
            }
            
            pdfDoc.close();
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
            byte[] bytes = file.getBytes();
            PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(bytes));
            PdfDocument pdfDoc = new PdfDocument(reader);
            
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
                pages.add(pageText);
            }
            
            pdfDoc.close();
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
