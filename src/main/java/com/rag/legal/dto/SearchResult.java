package com.rag.legal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    private Long id;
    private String documentNumber;
    private String title;
    private String content;
    private String tribunal;
    private String legalArea;
    private String status;
    private String documentType;
    private String publicationDate;
    private double score;
    private double rerankScore;
    private Map<String, Object> metadata;
}
