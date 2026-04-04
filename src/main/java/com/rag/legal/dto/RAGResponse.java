package com.rag.legal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RAGResponse {
    private String query;
    private String answer;
    private List<SearchResult> sources;
    private long processingTimeMs;
    private int sourceCount;
}
