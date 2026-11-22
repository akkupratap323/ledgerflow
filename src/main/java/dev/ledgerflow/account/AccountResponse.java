package dev.ledgerflow.account;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String code,
        String name,
        AccountType type,
        String currency,
        long balanceMinor,
        String balance,
        AccountStatus status,
        boolean allowNegative,
        Instant createdAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(), a.getCode(), a.getName(), a.getType(), a.getCurrency(),
                a.getBalanceMinor(), a.balance().format(), a.getStatus(),
                a.isAllowNegative(), a.getCreatedAt());
    }
}
