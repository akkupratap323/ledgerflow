package dev.ledgerflow.journal;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.account.AccountRepository;
import dev.ledgerflow.common.BusinessRuleException;
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
        EntryValidator.validate(command.lines());
        String currency = command.currency().toUpperCase();

        Map<String, Account> locked = lockAccounts(command.lines());

        JournalEntry entry = new JournalEntry(command.reference(), command.description(), currency);
        int lineNo = 1;
        for (PostingLine line : command.lines()) {
            Account account = locked.get(line.accountCode());
            if (!account.getCurrency().equals(currency)) {
                throw new BusinessRuleException("currency_mismatch",
                        "Account " + account.getCode() + " is in " + account.getCurrency()
                                + " but the entry is in " + currency);
            }
            account.applyPosting(line.side(), line.amountMinor());
            entry.addPosting(new Posting(entry, account, lineNo++, line.side(), line.amountMinor()));
        }
        return entries.save(entry);
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
            Account account = accounts.findByCode(code).orElseThrow(() -> new NotFoundException("Account", code));
            idToCode.put(account.getId(), code);
        }
        Map<String, Account> locked = new HashMap<>();
        idToCode.keySet().stream().sorted(Comparator.naturalOrder()).forEach(id ->
                locked.put(idToCode.get(id), accounts.findByIdForUpdate(id).orElseThrow()));
        return locked;
    }
}
