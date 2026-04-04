package com.rag.legal.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legal_documents", indexes = {
    @Index(name = "idx_document_number", columnList = "documentNumber"),
    @Index(name = "idx_tribunal", columnList = "tribunal"),
    @Index(name = "idx_legal_area", columnList = "legalArea"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificação
    @Column(nullable = false, unique = true)
    private String documentNumber;

    @Column(nullable = false)
    private String documentType; // SUMULA, LEI, DECRETO, RESOLUCAO

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Metadados Temporais
    @Column(nullable = false)
    private LocalDate publicationDate;

    @Column
    private LocalDate revocationDate;

    @Column(nullable = false)
    private String status; // VIGENTE, REVOGADA, PARCIALMENTE_REVOGADA

    // Metadados de Classificação
    @Column(nullable = false)
    private String legalArea; // CIVIL, PENAL, TRABALHISTA, ADMINISTRATIVO

    @Column(nullable = false)
    private String tribunal; // STF, STJ, TJ, TST, TSE

    @Column
    private String chapter;

    @Column
    private String article;

    @Column
    private String paragraph;

    // Metadados de Estrutura
    @Column
    private String section;

    @Column
    private Integer hierarchyLevel;

    // Embeddings
    @Column(columnDefinition = "FLOAT ARRAY")
    private float[] embedding;

    // Busca BM25
    @Column(columnDefinition = "TEXT")
    private String bm25Index;

    // Auditoria
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Retorna os metadados como um mapa estruturado para filtragem
     */
    public java.util.Map<String, Object> getMetadata() {
        return java.util.Map.ofEntries(
            java.util.Map.entry("documentNumber", this.documentNumber),
            java.util.Map.entry("documentType", this.documentType),
            java.util.Map.entry("tribunal", this.tribunal),
            java.util.Map.entry("legalArea", this.legalArea),
            java.util.Map.entry("status", this.status),
            java.util.Map.entry("publicationDate", this.publicationDate.toString()),
            java.util.Map.entry("chapter", this.chapter),
            java.util.Map.entry("article", this.article),
            java.util.Map.entry("paragraph", this.paragraph)
        );
    }

    /**
     * Retorna o texto completo para indexação BM25
     */
    public String getFullTextForIndexing() {
        return String.format("%s %s %s %s %s %s",
            this.documentNumber,
            this.title,
            this.content,
            this.legalArea,
            this.tribunal,
            this.documentType
        ).toLowerCase();
    }
}
