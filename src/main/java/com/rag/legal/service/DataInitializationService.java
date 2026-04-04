package com.rag.legal.service;

import com.rag.legal.domain.LegalDocument;
import com.rag.legal.repository.LegalDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private LegalDocumentRepository documentRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private BM25SearchService bm25SearchService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando carregamento de dados de exemplo...");

        if (documentRepository.count() == 0) {
            List<LegalDocument> documents = createSampleDocuments();
            documentRepository.saveAll(documents);

            // Indexar documentos para BM25
            for (LegalDocument doc : documents) {
                // Gerar embedding
                float[] embedding = embeddingService.embed(doc.getTitle() + " " + doc.getContent());
                doc.setEmbedding(embedding);
                documentRepository.save(doc);

                // Indexar para BM25
                bm25SearchService.indexDocument(doc);
            }

            log.info("Carregados {} documentos de exemplo", documents.size());
        } else {
            log.info("Documentos já existem no banco de dados");
        }
    }

    private List<LegalDocument> createSampleDocuments() {
        List<LegalDocument> documents = new ArrayList<>();

        // Súmula 1 - STF
        documents.add(LegalDocument.builder()
            .documentNumber("STF-SUM-735")
            .documentType("SUMULA")
            .title("Súmula 735 - Execução Penal")
            .content("Não se admite o isolamento carcerário do sentenciado senão por necessidade disciplinar ou de segurança penitenciária, mediante processo administrativo em que lhe seja assegurada a defesa técnica.")
            .publicationDate(LocalDate.of(2003, 11, 13))
            .status("VIGENTE")
            .legalArea("PENAL")
            .tribunal("STF")
            .chapter("EXECUÇÃO PENAL")
            .article("1")
            .build());

        // Lei 1 - Código Penal
        documents.add(LegalDocument.builder()
            .documentNumber("LEI-1940-CP")
            .documentType("LEI")
            .title("Lei 2.848/1940 - Código Penal Brasileiro")
            .content("Art. 1º - Não há crime sem lei anterior que o defina. Não há pena sem prévia cominação legal. Art. 2º - Ninguém pode ser punido por fato que lei posterior deixa de considerar crime, cessando em virtude dela a execução e os efeitos penais da sentença condenatória.")
            .publicationDate(LocalDate.of(1940, 12, 7))
            .status("VIGENTE")
            .legalArea("PENAL")
            .tribunal("STF")
            .chapter("PRINCÍPIOS FUNDAMENTAIS")
            .article("1")
            .paragraph("1")
            .build());

        // Lei 2 - Código Civil
        documents.add(LegalDocument.builder()
            .documentNumber("LEI-2002-CC")
            .documentType("LEI")
            .title("Lei 10.406/2002 - Código Civil Brasileiro")
            .content("Art. 1º - Toda pessoa é capaz de direitos e deveres na ordem civil. Art. 2º - A personalidade civil da pessoa começa do nascimento com vida; mas a lei põe a salvo, desde a concepção, os direitos do nascituro.")
            .publicationDate(LocalDate.of(2002, 1, 10))
            .status("VIGENTE")
            .legalArea("CIVIL")
            .tribunal("STJ")
            .chapter("PESSOAS")
            .article("1")
            .build());

        // Súmula 2 - STJ
        documents.add(LegalDocument.builder()
            .documentNumber("STJ-SUM-7")
            .documentType("SUMULA")
            .title("Súmula 7 - Prova Pericial")
            .content("A pretensão de simples reexame de prova não enseja recurso especial.")
            .publicationDate(LocalDate.of(1990, 4, 3))
            .status("VIGENTE")
            .legalArea("PROCESSUAL")
            .tribunal("STJ")
            .chapter("RECURSOS")
            .article("1")
            .build());

        // Lei 3 - Lei de Execução Penal
        documents.add(LegalDocument.builder()
            .documentNumber("LEI-7210-LEP")
            .documentType("LEI")
            .title("Lei 7.210/1984 - Lei de Execução Penal")
            .content("Art. 1º - A execução penal tem por objetivo efetivar as disposições de sentença ou decisão criminal e proporcionar condições para a harmônica integração social do condenado e do internado. Art. 2º - A jurisdição penal executória é exercida pela justiça estadual, em primeiro grau, na forma da lei de organização judiciária da União, dos Estados e do Distrito Federal.")
            .publicationDate(LocalDate.of(1984, 7, 11))
            .status("VIGENTE")
            .legalArea("PENAL")
            .tribunal("STF")
            .chapter("DISPOSIÇÕES PRELIMINARES")
            .article("1")
            .build());

        // Lei 4 - Lei de Direito Autoral
        documents.add(LegalDocument.builder()
            .documentNumber("LEI-9610-DA")
            .documentType("LEI")
            .title("Lei 9.610/1998 - Lei de Direito Autoral")
            .content("Art. 1º - Esta Lei regula os direitos autorais, entendendo-se sob esta denominação os direitos de autor e os que lhe são conexos. Art. 2º - Os autores de obras intelectuais são protegidos pelas disposições desta Lei.")
            .publicationDate(LocalDate.of(1998, 2, 19))
            .status("VIGENTE")
            .legalArea("CIVIL")
            .tribunal("STJ")
            .chapter("DISPOSIÇÕES PRELIMINARES")
            .article("1")
            .build());

        // Súmula 3 - STF sobre Prisão
        documents.add(LegalDocument.builder()
            .documentNumber("STF-SUM-1")
            .documentType("SUMULA")
            .title("Súmula 1 - Prisão em Segunda Instância")
            .content("Não é admissível o recurso extraordinário, quando a decisão recorrida assenta em mais de um fundamento suficiente e o recurso é dirigido contra um deles.")
            .publicationDate(LocalDate.of(1964, 5, 13))
            .status("VIGENTE")
            .legalArea("PROCESSUAL")
            .tribunal("STF")
            .chapter("RECURSOS")
            .article("1")
            .build());

        // Lei 5 - Lei de Acesso à Informação
        documents.add(LegalDocument.builder()
            .documentNumber("LEI-12527-LAI")
            .documentType("LEI")
            .title("Lei 12.527/2011 - Lei de Acesso à Informação")
            .content("Art. 1º - Esta Lei dispõe sobre os procedimentos a serem observados pela União, Estados, Distrito Federal e Municípios, com o fim de garantir o acesso a informações previsto no inciso XXXIII do art. 5º, no inciso II do § 3º do art. 37 e no § 2º do art. 216 da Constituição Federal.")
            .publicationDate(LocalDate.of(2011, 11, 18))
            .status("VIGENTE")
            .legalArea("ADMINISTRATIVO")
            .tribunal("STJ")
            .chapter("DISPOSIÇÕES GERAIS")
            .article("1")
            .build());

        return documents;
    }
}
