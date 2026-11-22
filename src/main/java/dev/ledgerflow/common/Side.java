package dev.ledgerflow.common;

/** The two sides of a double-entry posting. */
public enum Side {
    DEBIT,
    CREDIT;

    public Side opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
