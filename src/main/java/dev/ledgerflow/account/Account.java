package dev.ledgerflow.account;

import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.common.Money;
import dev.ledgerflow.common.Side;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Column(name = "allow_negative", nullable = false)
    private boolean allowNegative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
        // for JPA
    }

    public Account(String code, String name, AccountType type, String currency, boolean allowNegative) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.type = type;
        this.currency = currency.toUpperCase();
        this.allowNegative = allowNegative;
        this.createdAt = Instant.now();
    }

    /**
     * Applies a posting to the cached balance. Returns the new balance.
     * Throws when the account may not go negative and the posting would overdraw it.
     */
    public long applyPosting(Side side, long amountMinor) {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("account_" + status.name().toLowerCase(),
                    "Account " + code + " is " + status.name().toLowerCase() + " and cannot take postings");
        }
        long next = Math.addExact(balanceMinor, type.signedEffect(side, amountMinor));
        if (next < 0 && !allowNegative) {
            throw new BusinessRuleException("insufficient_funds",
                    "Account " + code + " has insufficient funds");
        }
        balanceMinor = next;
        return next;
    }

    public void freeze() {
        requireNotClosed();
        status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        requireNotClosed();
        status = AccountStatus.ACTIVE;
    }

    public void close() {
        requireNotClosed();
        if (balanceMinor != 0) {
            throw new BusinessRuleException("balance_not_zero",
                    "Account " + code + " can only be closed at zero balance");
        }
        status = AccountStatus.CLOSED;
    }

    private void requireNotClosed() {
        if (status == AccountStatus.CLOSED) {
            throw new BusinessRuleException("account_closed", "Account " + code + " is closed");
        }
    }

    public AccountStatus getStatus() {
        return status;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AccountType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public Money balance() {
        return Money.of(balanceMinor, currency);
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
