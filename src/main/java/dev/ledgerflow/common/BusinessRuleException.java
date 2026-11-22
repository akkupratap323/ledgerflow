package dev.ledgerflow.common;

/** A request that is well formed but breaks a ledger rule (unbalanced entry, insufficient funds...). */
public class BusinessRuleException extends LedgerException {

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
