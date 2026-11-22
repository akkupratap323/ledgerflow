package dev.ledgerflow.hold;

public enum HoldStatus {
    PENDING,
    CAPTURED,
    VOIDED,
    EXPIRED;

    public boolean isFinal() {
        return this != PENDING;
    }
}
