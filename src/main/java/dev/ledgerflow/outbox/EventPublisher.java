package dev.ledgerflow.outbox;

/** Delivers one outbox event. Throwing means the delivery failed and will be retried. */
public interface EventPublisher {

    void publish(OutboxEvent event) throws Exception;
}
