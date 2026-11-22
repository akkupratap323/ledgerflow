package dev.ledgerflow.journal;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.common.Side;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One debit or credit line of a journal entry against a single account. */
@Entity
@Table(name = "postings")
public class Posting {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id")
    private JournalEntry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Side side;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "account_seq", nullable = false)
    private long accountSeq;

    @Column(name = "balance_after_minor", nullable = false)
    private long balanceAfterMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Posting() {
        // for JPA
    }

    Posting(JournalEntry entry, Account account, int lineNo, Side side, long amountMinor,
            long accountSeq, long balanceAfterMinor) {
        this.id = UUID.randomUUID();
        this.entry = entry;
        this.account = account;
        this.lineNo = lineNo;
        this.side = side;
        this.amountMinor = amountMinor;
        this.accountSeq = accountSeq;
        this.balanceAfterMinor = balanceAfterMinor;
        this.createdAt = entry.getCreatedAt();
    }

    public UUID getId() {
        return id;
    }

    public JournalEntry getEntry() {
        return entry;
    }

    public Account getAccount() {
        return account;
    }

    public int getLineNo() {
        return lineNo;
    }

    public Side getSide() {
        return side;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public long getAccountSeq() {
        return accountSeq;
    }

    public long getBalanceAfterMinor() {
        return balanceAfterMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
