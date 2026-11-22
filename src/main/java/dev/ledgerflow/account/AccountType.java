package dev.ledgerflow.account;

import dev.ledgerflow.common.Side;

/**
 * Standard accounting categories. Each one has a normal balance side:
 * the side that increases the balance.
 */
public enum AccountType {
    ASSET(Side.DEBIT),
    EXPENSE(Side.DEBIT),
    LIABILITY(Side.CREDIT),
    EQUITY(Side.CREDIT),
    REVENUE(Side.CREDIT);

    private final Side normalSide;

    AccountType(Side normalSide) {
        this.normalSide = normalSide;
    }

    public Side normalSide() {
        return normalSide;
    }

    /** Signed effect of a posting of {@code amount} on {@code side} for this account type. */
    public long signedEffect(Side side, long amount) {
        return side == normalSide ? amount : -amount;
    }
}
