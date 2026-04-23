package com.example.search.queue;

public record IndexingTask(String documentId, String tenantId, Action action) {
    public enum Action { UPSERT, DELETE }
}
