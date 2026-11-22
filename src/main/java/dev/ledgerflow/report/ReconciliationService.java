package dev.ledgerflow.report;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.account.AccountRepository;
import dev.ledgerflow.common.Side;
import dev.ledgerflow.journal.PostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recomputes every balance from the raw postings and compares it with the cached
 * balance on the account. Any difference means a bug or a manual database edit.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    public record Mismatch(UUID accountId, String code, long cachedMinor, long computedMinor,
                           long cachedPostingCount, long actualPostingCount) {
    }

    public record Report(int accountsChecked, List<Mismatch> mismatches) {
        public boolean clean() {
            return mismatches.isEmpty();
        }
    }

    private final AccountRepository accounts;
    private final PostingRepository postings;

    public ReconciliationService(AccountRepository accounts, PostingRepository postings) {
        this.accounts = accounts;
        this.postings = postings;
    }

    @Transactional(readOnly = true)
    public Report reconcile() {
        Map<UUID, long[]> computed = new HashMap<>();
        for (Object[] row : postings.netDebitsByAccount()) {
            computed.put((UUID) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        List<Mismatch> mismatches = new ArrayList<>();
        List<Account> all = accounts.findAll();
        for (Account a : all) {
            long[] c = computed.getOrDefault(a.getId(), new long[]{0, 0});
            long balance = a.getType().normalSide() == Side.DEBIT ? c[0] : -c[0];
            if (balance != a.getBalanceMinor() || c[1] != a.getPostingCount()) {
                mismatches.add(new Mismatch(a.getId(), a.getCode(), a.getBalanceMinor(), balance,
                        a.getPostingCount(), c[1]));
            }
        }
        if (!mismatches.isEmpty()) {
            log.error("Reconciliation found {} mismatched accounts: {}", mismatches.size(), mismatches);
        }
        return new Report(all.size(), mismatches);
    }
}
