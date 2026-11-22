package dev.ledgerflow.journal;

import com.jayway.jsonpath.JsonPath;
import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.common.ConflictException;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReversalTest {

    @Autowired
    LedgerService ledger;
    @Autowired
    AccountService accountService;
    @Autowired
    MockMvc mvc;

    String cash;
    String alice;
    String bob;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accountService);
        cash = t.asset("cash");
        alice = t.wallet("alice");
        bob = t.wallet("bob");
    }

    @Test
    void reversalRestoresBalances() {
        JournalEntry topUp = ledger.post(cmd(debit(cash, 3_000), credit(alice, 3_000)));

        JournalEntry reversal = ledger.reverse(topUp.getId(), "duplicate top-up");

        assertThat(reversal.getReversalOf()).isEqualTo(topUp.getId());
        assertThat(reversal.getDescription()).contains("duplicate top-up");
        assertThat(balance(cash)).isZero();
        assertThat(balance(alice)).isZero();
    }

    @Test
    void entryCanOnlyBeReversedOnce() {
        JournalEntry topUp = ledger.post(cmd(debit(cash, 3_000), credit(alice, 3_000)));
        ledger.reverse(topUp.getId(), null);

        assertThatThrownBy(() -> ledger.reverse(topUp.getId(), null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void reversalCannotBeReversed() {
        JournalEntry topUp = ledger.post(cmd(debit(cash, 3_000), credit(alice, 3_000)));
        JournalEntry reversal = ledger.reverse(topUp.getId(), null);

        assertThatThrownBy(() -> ledger.reverse(reversal.getId(), null))
                .hasMessageContaining("cannot itself be reversed");
    }

    @Test
    void reversalFailsWhenMoneyWasAlreadySpent() {
        JournalEntry topUp = ledger.post(cmd(debit(cash, 3_000), credit(alice, 3_000)));
        ledger.post(cmd(debit(alice, 2_000), credit(bob, 2_000)));

        assertThatThrownBy(() -> ledger.reverse(topUp.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insufficient funds");
    }

    @Test
    void reversalOverHttp() throws Exception {
        JournalEntry topUp = ledger.post(cmd(debit(cash, 500), credit(alice, 500)));

        String body = mvc.perform(post("/api/v1/entries/" + topUp.getId() + "/reversal")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"customer refund\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reversalOf").value(topUp.getId().toString()))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(body, "$.postings[0].side")).isEqualTo("CREDIT");

        mvc.perform(post("/api/v1/entries/" + topUp.getId() + "/reversal"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_reversed"));
    }

    private static PostEntryCommand cmd(PostingLine... lines) {
        return new PostEntryCommand("ref", null, "INR", List.of(lines));
    }

    private long balance(String code) {
        return accountService.getByCode(code).getBalanceMinor();
    }
}
