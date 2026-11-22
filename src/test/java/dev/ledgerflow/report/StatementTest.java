package dev.ledgerflow.report;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatementTest {

    @Autowired
    StatementService statements;
    @Autowired
    AccountService accounts;
    @Autowired
    LedgerService ledger;
    @Autowired
    MockMvc mvc;

    UUID walletId;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accounts);
        String cash = t.asset("cash");
        String wallet = t.wallet("wallet");
        String shop = t.wallet("shop");
        walletId = accounts.getByCode(wallet).getId();
        ledger.post(new PostEntryCommand("t1", "Top up", "INR", List.of(debit(cash, 1_000), credit(wallet, 1_000))));
        ledger.post(new PostEntryCommand("p1", "Coffee", "INR", List.of(debit(wallet, 250), credit(shop, 250))));
        ledger.post(new PostEntryCommand("p2", "Lunch", "INR", List.of(debit(wallet, 400), credit(shop, 400))));
    }

    @Test
    void linesAreInOrderWithRunningBalance() {
        Statement s = statements.statement(walletId, null, null, 0, 100);

        assertThat(s.totalLines()).isEqualTo(3);
        assertThat(s.lines()).extracting(StatementLine::seq).containsExactly(1L, 2L, 3L);
        assertThat(s.lines()).extracting(StatementLine::balanceAfterMinor).containsExactly(1_000L, 750L, 350L);
        assertThat(s.lines()).extracting(StatementLine::description).containsExactly("Top up", "Coffee", "Lunch");
    }

    @Test
    void paginates() {
        Statement page1 = statements.statement(walletId, null, null, 1, 2);

        assertThat(page1.lines()).hasSize(1);
        assertThat(page1.lines().get(0).reference()).isEqualTo("p2");
    }

    @Test
    void futureRangeIsEmpty() {
        Instant from = Instant.now().plusSeconds(3600);
        Statement s = statements.statement(walletId, from, from.plusSeconds(60), 0, 10);

        assertThat(s.lines()).isEmpty();
    }

    @Test
    void overHttpWithValidation() throws Exception {
        mvc.perform(get("/api/v1/accounts/" + walletId + "/statement").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.totalLines").value(3));

        mvc.perform(get("/api/v1/accounts/" + walletId + "/statement").param("size", "9999"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("invalid_page"));
    }
}
