package com.rag.legal.service;

import com.rag.legal.domain.LegalDocument;
import com.rag.legal.dto.FileMultipartFile;
import com.rag.legal.repository.LegalDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private PDFIndexingService pdfIndexingService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando carregamento de dados iniciais...");

        String folderPath = "./knowledge_base";
        File folder = new File(folderPath);

            if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("O caminho fornecido não é uma pasta válida ou não existe.");
            return;
        }
        System.out.println("Iniciando extração de texto dos PDFs em: " + folderPath);

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        System.out.println("Lendo arquivo: " + path.getFileName());
                        pdfIndexingService.indexPDF(new FileMultipartFile(path));
                    });
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
