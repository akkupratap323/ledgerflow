package dev.ledgerflow.common;

/** Base type for errors the API turns into a structured problem response. */
public abstract class LedgerException extends RuntimeException {

    private final String errorCode;

    protected LedgerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
