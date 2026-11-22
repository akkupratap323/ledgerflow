package dev.ledgerflow.journal;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.account.AccountRepository;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.common.ConflictException;
import dev.ledgerflow.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Writes balanced journal entries and keeps account balances in step, atomically. */
@Service
public class LedgerService {

    private final AccountRepository accounts;
    private final JournalEntryRepository entries;

    public LedgerService(AccountRepository accounts, JournalEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    @Transactional
    public JournalEntry post(PostEntryCommand command) {
        return post(command, null).entry();
    }

    /**
     * Posts an entry. With an idempotency key, a retry of the same request returns the original
     * entry and a different request with the same key is rejected.
     */
    @Transactional
    public PostResult post(PostEntryCommand command, String idempotencyKey) {
        EntryValidator.validate(command.lines());
        String hash = RequestHasher.hash(command);
        if (idempotencyKey != null) {
            var existing = entries.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return new PostResult(checkReplay(existing.get(), hash), true);
            }
        }
        JournalEntry entry = new JournalEntry(command.reference(), command.description(), command.currency());
        if (idempotencyKey != null) {
            entry.bindIdempotencyKey(idempotencyKey, hash);
        }
        return new PostResult(write(entry, command.lines()), false);
    }

    /**
     * Reverses an entry by posting its mirror image. The original stays untouched, so the
     * history keeps both the mistake and the correction.
     */
    @Transactional
    public JournalEntry reverse(UUID entryId, String reason) {
        JournalEntry original = entries.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Journal entry", entryId));
        if (original.getReversalOf() != null) {
            throw new BusinessRuleException("cannot_reverse_reversal", "A reversal entry cannot itself be reversed");
        }
        if (entries.existsByReversalOf(entryId)) {
            throw new ConflictException("already_reversed", "Entry " + entryId + " was already reversed");
        }
        List<PostingLine> mirrored = original.getPostings().stream()
                .map(p -> new PostingLine(p.getAccount().getCode(), p.getSide().opposite(), p.getAmountMinor()))
                .toList();
        String description = "Reversal of " + entryId + (reason == null ? "" : ": " + reason);
        JournalEntry reversal = new JournalEntry(original.getReference(), description, original.getCurrency());
        reversal.markAsReversalOf(entryId);
        return write(reversal, mirrored);
    }

    private JournalEntry write(JournalEntry entry, List<PostingLine> lines) {
        String currency = entry.getCurrency();
        Map<String, Account> locked = lockAccounts(lines);
        int lineNo = 1;
        for (PostingLine line : lines) {
            Account account = locked.get(line.accountCode());
            if (!account.getCurrency().equals(currency)) {
                throw new BusinessRuleException("currency_mismatch",
                        "Account " + account.getCode() + " is in " + account.getCurrency()
                                + " but the entry is in " + currency);
            }
            account.applyPosting(line.side(), line.amountMinor());
            entry.addPosting(new Posting(entry, account, lineNo++, line.side(), line.amountMinor()));
        }
        return entries.saveAndFlush(entry);
    }

    /** Used after losing an insert race on the idempotency key. */
    @Transactional(readOnly = true)
    public PostResult replay(PostEntryCommand command, String idempotencyKey) {
        JournalEntry existing = entries.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new NotFoundException("Journal entry with idempotency key", idempotencyKey));
        checkReplay(existing, RequestHasher.hash(command));
        existing.getPostings().forEach(p -> p.getAccount().getCode());
        return new PostResult(existing, true);
    }

    private static JournalEntry checkReplay(JournalEntry existing, String hash) {
        if (!existing.getRequestHash().equals(hash)) {
            throw new ConflictException("idempotency_key_reused",
                    "Idempotency key was already used for a different request");
        }
        existing.getPostings().forEach(p -> p.getAccount().getCode());
        return existing;
    }

    @Transactional(readOnly = true)
    public JournalEntry get(UUID id) {
        JournalEntry entry = entries.findById(id).orElseThrow(() -> new NotFoundException("Journal entry", id));
        entry.getPostings().forEach(p -> p.getAccount().getCode()); // initialize for the response
        return entry;
    }

    /**
     * Locks every account touched by the entry. Locks are taken in id order so two entries
     * touching the same accounts can never deadlock each other.
     */
    private Map<String, Account> lockAccounts(List<PostingLine> lines) {
        Map<UUID, String> idToCode = new HashMap<>();
        for (String code : new LinkedHashSet<>(lines.stream().map(PostingLine::accountCode).toList())) {
            UUID id = accounts.findIdByCode(code).orElseThrow(() -> new NotFoundException("Account", code));
            idToCode.put(id, code);
        }
        Map<String, Account> locked = new HashMap<>();
        idToCode.keySet().stream().sorted(Comparator.naturalOrder()).forEach(id ->
                locked.put(idToCode.get(id), accounts.findByIdForUpdate(id).orElseThrow()));
        return locked;
    }
}
