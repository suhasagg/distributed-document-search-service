package com.example.search.messaging;

import com.example.search.domain.DocumentEntity;
import com.example.search.repository.DocumentRepository;
import com.example.search.service.ElasticsearchIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaIndexConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaIndexConsumer.class);

    private final DocumentRepository documentRepository;
    private final ElasticsearchIndexService elasticsearchIndexService;

    public KafkaIndexConsumer(DocumentRepository documentRepository,
                              ElasticsearchIndexService elasticsearchIndexService) {
        this.documentRepository = documentRepository;
        this.elasticsearchIndexService = elasticsearchIndexService;
    }

    @KafkaListener(
            topics = "${indexing.topic.documents}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(IndexingEvent event) {
        try {
            if (event.action() == IndexingEvent.Action.DELETE) {
                elasticsearchIndexService.delete(event.documentId());
                log.info("Deleted document from index documentId={}", event.documentId());
                return;
            }

            DocumentEntity entity = documentRepository.findById(event.documentId()).orElse(null);
            if (entity == null) {
                log.warn("Document not found at indexing time, documentId={}", event.documentId());
                return;
            }

            elasticsearchIndexService.index(entity);
            log.info("Indexed event action={} documentId={}", event.action(), event.documentId());
        } catch (Exception ex) {
            log.error("Indexing consumer failure for document {}", event.documentId(), ex);
        }
    }
}
