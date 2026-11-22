package dev.ledgerflow.common;

public class NotFoundException extends LedgerException {

    public NotFoundException(String resource, Object id) {
        super("not_found", resource + " " + id + " was not found");
    }
}
