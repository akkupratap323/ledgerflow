package dev.ledgerflow.common;

public class ConflictException extends LedgerException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
