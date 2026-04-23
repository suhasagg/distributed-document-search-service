package com.example.search.service;

import com.example.search.domain.DocumentEntity;
import com.example.search.dto.CreateDocumentRequest;
import com.example.search.messaging.IndexingEventPublisher;
import com.example.search.repository.DocumentRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final IndexingEventPublisher indexingEventPublisher;
    private final CacheService cacheService;

    public DocumentService(DocumentRepository documentRepository,
                           IndexingEventPublisher indexingEventPublisher,
                           CacheService cacheService) {
        this.documentRepository = documentRepository;
        this.indexingEventPublisher = indexingEventPublisher;
        this.cacheService = cacheService;
    }

    public DocumentEntity create(String tenantId, CreateDocumentRequest request) {
        DocumentEntity entity = new DocumentEntity();
        entity.setTenantId(tenantId);
        entity.setTitle(request.title());
        entity.setContent(request.content());

        DocumentEntity saved = documentRepository.saveAndFlush(entity);

        cacheService.evictTenantSearches(saved.getTenantId());

        // publish only after the saveAndFlush call has completed outside a service transaction
        indexingEventPublisher.publishUpsert(saved.getId(), saved.getTenantId());

        return saved;
    }

    public Optional<DocumentEntity> getById(String tenantId, String id) {
        return documentRepository.findById(id)
                .filter(doc -> doc.getTenantId().equals(tenantId));
    }

    public boolean delete(String tenantId, String id) {
        Optional<DocumentEntity> existing = documentRepository.findById(id)
                .filter(doc -> doc.getTenantId().equals(tenantId));

        if (existing.isEmpty()) {
            return false;
        }

        DocumentEntity entity = existing.get();

        documentRepository.delete(entity);
        documentRepository.flush();

        cacheService.evictTenantSearches(entity.getTenantId());

        indexingEventPublisher.publishDelete(entity.getId(), entity.getTenantId());

        return true;
    }
}
