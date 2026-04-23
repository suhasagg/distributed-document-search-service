package com.example.search.dto;

import com.example.search.domain.DocumentEntity;
import java.time.Instant;

public record DocumentResponse(
        String id,
        String tenantId,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(DocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
