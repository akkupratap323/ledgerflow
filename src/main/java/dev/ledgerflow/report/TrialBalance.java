package dev.ledgerflow.report;

import dev.ledgerflow.account.AccountType;

import java.util.Map;

/**
 * Per currency: debit-normal balances (assets, expenses) must equal
 * credit-normal balances (liabilities, equity, revenue). If not, the ledger is broken.
 */
public record TrialBalance(String currency, Map<AccountType, Long> totalsByType,
                           long debitTotal, long creditTotal, boolean balanced) {
}
