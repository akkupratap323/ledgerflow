package dev.ledgerflow.hold;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.account.AccountRepository;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.common.NotFoundException;
import dev.ledgerflow.journal.JournalEntry;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.journal.PostingLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Two-step payments: place a hold now, then capture (fully or partly) or void it later.
 * Mirrors how card authorizations and marketplace escrow work.
 */
@Service
public class HoldService {

    static final Duration MAX_TTL = Duration.ofDays(30);

    private final HoldRepository holds;
    private final AccountRepository accounts;
    private final LedgerService ledger;

    public HoldService(HoldRepository holds, AccountRepository accounts, LedgerService ledger) {
        this.holds = holds;
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @Transactional
    public Hold place(String accountCode, long amountMinor, String reference, Duration ttl) {
        if (amountMinor <= 0) {
            throw new BusinessRuleException("invalid_amount", "Hold amount must be positive");
        }
        if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(MAX_TTL) > 0) {
            throw new BusinessRuleException("invalid_ttl", "Hold TTL must be between 1 second and 30 days");
        }
        UUID accountId = accounts.findIdByCode(accountCode)
                .orElseThrow(() -> new NotFoundException("Account", accountCode));
        Account account = accounts.findByIdForUpdate(accountId).orElseThrow();
        account.reserve(amountMinor);
        return holds.save(new Hold(accountId, amountMinor, account.getCurrency(), reference,
                Instant.now().plus(ttl)));
    }

    /**
     * Captures up to the held amount into {@code destinationCode}. Any remainder is released.
     */
    @Transactional
    public Hold capture(UUID holdId, long amountMinor, String destinationCode) {
        Hold hold = holds.findByIdForUpdate(holdId).orElseThrow(() -> new NotFoundException("Hold", holdId));
        hold.requirePending();
        if (hold.getStatus() == HoldStatus.PENDING && hold.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("hold_expired", "Hold " + holdId + " has expired");
        }
        if (amountMinor <= 0 || amountMinor > hold.getAmountMinor()) {
            throw new BusinessRuleException("invalid_amount",
                    "Capture amount must be between 1 and the held amount " + hold.getAmountMinor());
        }
        UUID destinationId = accounts.findIdByCode(destinationCode)
                .orElseThrow(() -> new NotFoundException("Account", destinationCode));
        Account source = lockInOrder(hold.getAccountId(), destinationId);

        source.release(hold.getAmountMinor());
        JournalEntry entry = ledger.post(new PostEntryCommand(hold.getReference(),
                "Capture of hold " + holdId, hold.getCurrency(),
                List.of(PostingLine.debit(source.getCode(), amountMinor),
                        PostingLine.credit(destinationCode, amountMinor))));
        hold.resolve(HoldStatus.CAPTURED, entry.getId());
        return hold;
    }

    @Transactional
    public Hold voidHold(UUID holdId) {
        return release(holdId, HoldStatus.VOIDED);
    }

    @Transactional
    public Hold expire(UUID holdId) {
        return release(holdId, HoldStatus.EXPIRED);
    }

    @Transactional(readOnly = true)
    public Hold get(UUID holdId) {
        return holds.findById(holdId).orElseThrow(() -> new NotFoundException("Hold", holdId));
    }

    private Hold release(UUID holdId, HoldStatus outcome) {
        Hold hold = holds.findByIdForUpdate(holdId).orElseThrow(() -> new NotFoundException("Hold", holdId));
        hold.requirePending();
        Account account = accounts.findByIdForUpdate(hold.getAccountId()).orElseThrow();
        hold.resolve(outcome, null);
        account.release(hold.getAmountMinor());
        return hold;
    }

    /** Locks both accounts in id order, matching the ledger, so captures cannot deadlock with posts. */
    private Account lockInOrder(UUID sourceId, UUID destinationId) {
        Account source = null;
        for (UUID id : Stream.of(sourceId, destinationId).distinct().sorted().toList()) {
            Account locked = accounts.findByIdForUpdate(id).orElseThrow();
            if (id.equals(sourceId)) {
                source = locked;
            }
        }
        return source;
    }
}
