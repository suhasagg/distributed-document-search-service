package com.example.search.messaging;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class IndexingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IndexingEventPublisher.class);

    private final KafkaTemplate<String, IndexingEvent> kafkaTemplate;
    private final String topic;

    public IndexingEventPublisher(KafkaTemplate<String, IndexingEvent> kafkaTemplate,
                                  @Value("${indexing.topic.documents}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishUpsert(String documentId, String tenantId) {
        publish(new IndexingEvent(
                UUID.randomUUID().toString(),
                documentId,
                tenantId,
                IndexingEvent.Action.UPSERT,
                Instant.now()
        ));
    }

    public void publishDelete(String documentId, String tenantId) {
        publish(new IndexingEvent(
                UUID.randomUUID().toString(),
                documentId,
                tenantId,
                IndexingEvent.Action.DELETE,
                Instant.now()
        ));
    }

    private void publish(IndexingEvent event) {
        kafkaTemplate.send(topic, event.tenantId(), event)
                .whenComplete((SendResult<String, IndexingEvent> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish indexing event action={} documentId={}",
                                event.action(), event.documentId(), ex);
                    } else {
                        log.info("Published indexing event action={} documentId={} partition={} offset={}",
                                event.action(),
                                event.documentId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
