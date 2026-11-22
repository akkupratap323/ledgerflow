package dev.ledgerflow.report;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReconciliationTest {

    @Autowired
    ReconciliationService reconciliation;
    @Autowired
    AccountService accounts;
    @Autowired
    LedgerService ledger;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void detectsTamperedBalance() {
        TestAccounts t = new TestAccounts(accounts);
        String cash = t.asset("cash");
        String wallet = t.wallet("wallet");
        ledger.post(new PostEntryCommand("t", null, "INR", List.of(debit(cash, 700), credit(wallet, 700))));
        UUID walletId = accounts.getByCode(wallet).getId();

        assertThat(reconciliation.reconcile().mismatches())
                .noneMatch(m -> m.accountId().equals(walletId));

        // Someone "fixes" a balance directly in the database.
        jdbc.update("update accounts set balance_minor = 9999 where id = ?", walletId);
        try {
            assertThat(reconciliation.reconcile().mismatches())
                    .anySatisfy(m -> {
                        assertThat(m.accountId()).isEqualTo(walletId);
                        assertThat(m.cachedMinor()).isEqualTo(9999);
                        assertThat(m.computedMinor()).isEqualTo(700);
                    });
        } finally {
            jdbc.update("update accounts set balance_minor = 700 where id = ?", walletId);
        }
    }
}
