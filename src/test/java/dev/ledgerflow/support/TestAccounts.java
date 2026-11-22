package dev.ledgerflow.support;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.account.AccountType;

import java.util.UUID;

/** Opens accounts with unique codes so tests never collide on shared database state. */
public final class TestAccounts {

    private final AccountService accounts;
    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    public TestAccounts(AccountService accounts) {
        this.accounts = accounts;
    }

    public String asset(String name) {
        return open(name, AccountType.ASSET, true);
    }

    public String wallet(String name) {
        return open(name, AccountType.LIABILITY, false);
    }

    public String revenue(String name) {
        return open(name, AccountType.REVENUE, false);
    }

    public String open(String name, AccountType type, boolean allowNegative) {
        String code = name + "_" + suffix;
        accounts.open(code, name, type, "INR", allowNegative);
        return code;
    }
}
