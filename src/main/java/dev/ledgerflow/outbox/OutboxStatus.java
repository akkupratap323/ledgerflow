package dev.ledgerflow.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    /** Gave up after the maximum number of attempts. Needs a human to look at it. */
    DEAD
}
