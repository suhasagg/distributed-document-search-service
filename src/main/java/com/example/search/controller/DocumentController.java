package com.example.search.controller;

import com.example.search.domain.DocumentEntity;
import com.example.search.dto.CreateDocumentRequest;
import com.example.search.dto.DocumentResponse;
import com.example.search.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody CreateDocumentRequest request) {

        DocumentEntity saved = documentService.create(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(saved));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {

        return documentService.getById(tenantId, id)
                .map(document -> ResponseEntity.ok(DocumentResponse.from(document)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {

        boolean deleted = documentService.delete(tenantId, id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
