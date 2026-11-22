package dev.ledgerflow.journal;

import dev.ledgerflow.common.Side;

/** A requested posting before it is written to the ledger. */
public record PostingLine(String accountCode, Side side, long amountMinor) {

    public static PostingLine debit(String accountCode, long amountMinor) {
        return new PostingLine(accountCode, Side.DEBIT, amountMinor);
    }

    public static PostingLine credit(String accountCode, long amountMinor) {
        return new PostingLine(accountCode, Side.CREDIT, amountMinor);
    }
}
