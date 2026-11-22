package dev.ledgerflow.common;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount of money stored as a whole number of minor units (paise, cents).
 * Floating point is never used for money in the ledger.
 */
public record Money(long amountMinor, String currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        currency = currency.toUpperCase();
    }

    public static Money of(long amountMinor, String currency) {
        return new Money(amountMinor, currency);
    }

    public static Money zero(String currency) {
        return new Money(0, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amountMinor, other.amountMinor), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(amountMinor, other.amountMinor), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(amountMinor), currency);
    }

    public boolean isNegative() {
        return amountMinor < 0;
    }

    public boolean isPositive() {
        return amountMinor > 0;
    }

    public boolean isZero() {
        return amountMinor == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    /** Human readable form, e.g. "INR 120.50". */
    public String format() {
        int digits = Math.max(Currency.getInstance(currency).getDefaultFractionDigits(), 0);
        return currency + " " + BigDecimal.valueOf(amountMinor, digits).toPlainString();
    }
}
