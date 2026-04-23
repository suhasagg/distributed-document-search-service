package com.example.search.dto;

import java.util.List;

public record SearchResponse(
        String tenantId,
        String query,
        long total,
        List<SearchResultItem> results,
        boolean cacheHit
) {}
