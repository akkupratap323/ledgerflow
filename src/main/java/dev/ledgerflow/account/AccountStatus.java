package dev.ledgerflow.account;

public enum AccountStatus {
    /** Normal operation. */
    ACTIVE,
    /** Temporarily blocked, e.g. a fraud or KYC review. Can be unfrozen. */
    FROZEN,
    /** Permanently closed. Only allowed at zero balance. */
    CLOSED
}
