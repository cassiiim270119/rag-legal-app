package com.rag.legal.dto;

import java.util.Map;

public record IndexingRequest(
        String content,
        Map<String, String> metadata
) {
}
