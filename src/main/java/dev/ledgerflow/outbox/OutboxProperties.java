package dev.ledgerflow.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ledgerflow.outbox")
public record OutboxProperties(
        String webhookUrl,
        String webhookSecret,
        Integer batchSize,
        Integer maxAttempts,
        Duration baseBackoff,
        Duration maxBackoff
) {
    public OutboxProperties {
        batchSize = batchSize == null ? 50 : batchSize;
        maxAttempts = maxAttempts == null ? 8 : maxAttempts;
        baseBackoff = baseBackoff == null ? Duration.ofSeconds(5) : baseBackoff;
        maxBackoff = maxBackoff == null ? Duration.ofMinutes(30) : maxBackoff;
    }

    /** Exponential backoff: base * 2^(attempt-1), capped at maxBackoff. */
    public Duration backoffFor(int attempt) {
        long factor = 1L << Math.min(Math.max(attempt - 1, 0), 20);
        Duration d = baseBackoff.multipliedBy(factor);
        return d.compareTo(maxBackoff) > 0 ? maxBackoff : d;
    }
}
