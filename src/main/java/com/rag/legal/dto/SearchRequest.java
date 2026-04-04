package com.rag.legal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {
    private String query;
    private String tribunal;
    private String legalArea;
    private Integer limit;

    public Integer getLimitOrDefault() {
        return limit != null && limit > 0 ? Math.min(limit, 50) : 10;
    }
}
