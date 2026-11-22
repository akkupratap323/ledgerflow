package dev.ledgerflow.hold;

import dev.ledgerflow.common.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/** Funds reserved on an account. A hold ends captured, voided or expired. */
@Entity
@Table(name = "holds")
public class Hold {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HoldStatus status;

    @Column(length = 100)
    private String reference;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "capture_entry_id")
    private UUID captureEntryId;

    @Version
    private long version;

    protected Hold() {
        // for JPA
    }

    public Hold(UUID accountId, long amountMinor, String currency, String reference, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.reference = reference;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.status = HoldStatus.PENDING;
    }

    void requirePending() {
        if (status.isFinal()) {
            throw new BusinessRuleException("hold_" + status.name().toLowerCase(),
                    "Hold " + id + " is already " + status.name().toLowerCase());
        }
    }

    void resolve(HoldStatus outcome, UUID entryId) {
        requirePending();
        this.status = outcome;
        this.captureEntryId = entryId;
        this.resolvedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public UUID getCaptureEntryId() {
        return captureEntryId;
    }
}
