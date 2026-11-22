package dev.ledgerflow.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default publisher when no webhook URL is configured. Useful for local development. */
public class LoggingPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingPublisher.class);

    @Override
    public void publish(OutboxEvent event) {
        log.info("event {} {} for {} {}", event.getId(), event.getEventType(),
                event.getAggregateType(), event.getAggregateId());
    }
}
