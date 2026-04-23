package com.example.search.service;

import com.example.search.messaging.IndexingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexingDeadLetterService {

    private static final Logger log = LoggerFactory.getLogger(IndexingDeadLetterService.class);

    public void persistFailure(IndexingEvent event, Exception ex) {
        log.error("DLQ capture eventId={} documentId={} action={}",
                event.eventId(), event.documentId(), event.action(), ex);
    }
}
