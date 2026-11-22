package dev.ledgerflow.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * Polls the outbox and delivers due events at least once. Failures are retried with
 * exponential backoff; after max attempts the event is parked as DEAD.
 * Receivers should dedupe on the X-LedgerFlow-Event-Id header.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final EventPublisher publisher;
    private final OutboxProperties props;
    private final TransactionTemplate tx;

    public OutboxRelay(OutboxRepository repository, EventPublisher publisher, OutboxProperties props,
                       TransactionTemplate tx) {
        this.repository = repository;
        this.publisher = publisher;
        this.props = props;
        this.tx = tx;
    }

    @Scheduled(fixedDelayString = "${ledgerflow.outbox.poll-interval:PT2S}")
    public void run() {
        relayDue(Instant.now());
    }

    /** Delivers every event due at {@code now}. The HTTP call happens outside any transaction. */
    public int relayDue(Instant now) {
        int sent = 0;
        for (UUID id : repository.findDueIds(now, PageRequest.of(0, props.batchSize()))) {
            OutboxEvent event = repository.findById(id).orElse(null);
            if (event == null || event.getStatus() != OutboxStatus.PENDING) {
                continue;
            }
            String error = null;
            try {
                publisher.publish(event);
            } catch (Exception e) {
                error = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            String failure = error;
            tx.executeWithoutResult(status -> {
                OutboxEvent fresh = repository.findById(id).orElseThrow();
                if (failure == null) {
                    fresh.markSent(now);
                } else {
                    fresh.markFailed(now, failure, props.maxAttempts(), props.backoffFor(fresh.getAttempts() + 1));
                    log.warn("Delivery of event {} failed (attempt {}): {}", id, fresh.getAttempts(), failure);
                }
            });
            if (failure == null) {
                sent++;
            }
        }
        return sent;
    }
}
