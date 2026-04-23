package com.example.search.dto;

public record SearchResultItem(
        String id,
        String tenantId,
        String title,
        String snippet,
        double score
) {}
