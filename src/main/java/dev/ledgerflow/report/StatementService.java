package dev.ledgerflow.report;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.journal.Posting;
import dev.ledgerflow.journal.PostingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Builds account statements: every posting in order, with the balance after each one. */
@Service
public class StatementService {

    static final int MAX_PAGE_SIZE = 500;

    private final AccountService accounts;
    private final PostingRepository postings;

    public StatementService(AccountService accounts, PostingRepository postings) {
        this.accounts = accounts;
        this.postings = postings;
    }

    @Transactional(readOnly = true)
    public Statement statement(UUID accountId, Instant from, Instant to, int page, int size) {
        Instant start = from != null ? from : Instant.EPOCH;
        Instant end = to != null ? to : Instant.now().plusSeconds(1);
        if (!start.isBefore(end)) {
            throw new BusinessRuleException("invalid_range", "'from' must be before 'to'");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("invalid_page", "page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        }
        Account account = accounts.get(accountId);
        Page<Posting> result = postings.findStatement(accountId, start, end, PageRequest.of(page, size));
        return new Statement(account.getId(), account.getCode(), account.getCurrency(), start, end,
                page, size, result.getTotalElements(),
                result.map(p -> new StatementLine(p.getAccountSeq(), p.getEntry().getId(), p.getCreatedAt(),
                        p.getEntry().getReference(), p.getEntry().getDescription(), p.getSide(),
                        p.getAmountMinor(), p.getBalanceAfterMinor())).getContent());
    }
}
