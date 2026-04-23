package com.example.search.messaging;

import java.time.Instant;

public record IndexingEvent(
        String eventId,
        String documentId,
        String tenantId,
        Action action,
        Instant occurredAt
) {
    public enum Action {
        UPSERT, DELETE
    }
}
