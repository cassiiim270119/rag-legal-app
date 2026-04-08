package com.rag.legal.service;

import com.rag.legal.domain.LegalDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class BM25SearchService {

    private final Directory directory;
    private final Analyzer analyzer;
    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private DirectoryReader reader;

    public BM25SearchService() throws IOException {
        this.directory = new ByteBuffersDirectory();
        this.analyzer = new StandardAnalyzer();
        initializeIndexWriter();
        log.info("BM25SearchService inicializado com índice em memória");
    }

    private void initializeIndexWriter() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new BM25Similarity());
        this.indexWriter = new IndexWriter(directory, config);
    }

    /**
     * Indexa um documento jurídico
     */
    public void indexDocument(LegalDocument doc) throws IOException {
        Document luceneDoc = new Document();
        luceneDoc.add(new TextField("id", doc.getId().toString(), Field.Store.YES));
        luceneDoc.add(new TextField("documentNumber", doc.getDocumentNumber(), Field.Store.YES));
        luceneDoc.add(new TextField("title", doc.getTitle(), Field.Store.YES));
        luceneDoc.add(new TextField("content", doc.getContent(), Field.Store.YES));
        luceneDoc.add(new TextField("tribunal", doc.getTribunal(), Field.Store.YES));
        luceneDoc.add(new TextField("legalArea", doc.getLegalArea(), Field.Store.YES));
        luceneDoc.add(new TextField("status", doc.getStatus(), Field.Store.YES));
        luceneDoc.add(new TextField("documentType", doc.getDocumentType(), Field.Store.YES));

        // Campo combinado para busca geral
        String fullText = doc.getFullTextForIndexing();
        luceneDoc.add(new TextField("fullText", fullText, Field.Store.NO));

        indexWriter.addDocument(luceneDoc);
        log.debug("Documento indexado: {}", doc.getDocumentNumber());
    }

    /**
     * Indexa múltiplos documentos em batch
     */
    public void indexDocumentsBatch(List<LegalDocument> documents) throws IOException {
        for (LegalDocument doc : documents) {
            indexDocument(doc);
        }
        indexWriter.commit();
        refreshSearcher();
        log.info("Batch de {} documentos indexados", documents.size());
    }
    /**
     * Busca por palavras-chave usando BM25
     */
    public List<Map<String, Object>> search(String queryString, int limit) throws Exception {
        if (indexSearcher == null) {
            indexWriter.close();
            refreshSearcher();
        }

        QueryParser parser = new QueryParser("fullText", analyzer);
        Query query = parser.parse(queryString);

        TopDocs topDocs = indexSearcher.search(query, limit);
        List<Map<String, Object>> results = new ArrayList<>();

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            Map<String, Object> result = new HashMap<>();
            result.put("id", doc.get("id"));
            result.put("documentNumber", doc.get("documentNumber"));
            result.put("title", doc.get("title"));
            result.put("tribunal", doc.get("tribunal"));
            result.put("legalArea", doc.get("legalArea"));
            result.put("status", doc.get("status"));
            result.put("bm25Score", scoreDoc.score);
            results.add(result);
        }

        log.debug("Busca BM25 retornou {} resultados para query: {}", results.size(), queryString);
        return results;
    }

    /**
     * Busca com filtro de status (apenas vigentes)
     */
    public List<Map<String, Object>> searchVigente(String queryString, int limit) throws Exception {
        List<Map<String, Object>> allResults = search(queryString, limit * 2);
        return allResults.stream()
            .filter(r -> "VIGENTE".equals(r.get("status")))
            .limit(limit)
            .toList();
    }

    /**
     * Busca com filtro de tribunal
     */
    public List<Map<String, Object>> searchByTribunal(String queryString, String tribunal, int limit) throws Exception {
        List<Map<String, Object>> allResults = search(queryString, limit * 2);
        return allResults.stream()
            .filter(r -> tribunal.equals(r.get("tribunal")))
            .limit(limit)
            .toList();
    }

    /**
     * Atualiza o searcher após modificações no índice
     */
    private void refreshSearcher() throws IOException {
        if (indexSearcher != null && reader != null) {
            reader.close();
        }
        reader = DirectoryReader.open(directory);
        indexSearcher = new IndexSearcher(reader);
    }

    /**
     * Limpa o índice
     */
    public void clearIndex() throws IOException {
        indexWriter.deleteAll();
        indexWriter.commit();
        refreshSearcher();
        log.info("Índice BM25 limpo");
    }

    /**
     * Fecha recursos
     */
    public void close() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
        if (reader != null) {
            reader.close();
        }
        if (analyzer != null) {
            analyzer.close();
        }
    }
}
