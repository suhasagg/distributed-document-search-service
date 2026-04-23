package com.example.search.service;

import com.example.search.domain.DocumentEntity;
import com.example.search.repository.DocumentRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexerWorker {

    private static final Logger log = LoggerFactory.getLogger(IndexerWorker.class);

    private final DocumentRepository documentRepository;
    private final ElasticsearchIndexService elasticsearchIndexService;

    public IndexerWorker(DocumentRepository documentRepository,
                         ElasticsearchIndexService elasticsearchIndexService) {
        this.documentRepository = documentRepository;
        this.elasticsearchIndexService = elasticsearchIndexService;
    }

    public void upsert(String documentId) {
        try {
            Optional<DocumentEntity> entity = documentRepository.findById(documentId);
            if (entity.isPresent()) {
                elasticsearchIndexService.index(entity.get());
                log.info("IndexerWorker upserted documentId={}", documentId);
            } else {
                log.warn("IndexerWorker could not find documentId={}", documentId);
            }
        } catch (Exception ex) {
            log.error("IndexerWorker failed for documentId={}", documentId, ex);
        }
    }

    public void delete(String documentId) {
        try {
            elasticsearchIndexService.delete(documentId);
            log.info("IndexerWorker deleted documentId={}", documentId);
        } catch (Exception ex) {
            log.error("IndexerWorker delete failed for documentId={}", documentId, ex);
        }
    }
}
