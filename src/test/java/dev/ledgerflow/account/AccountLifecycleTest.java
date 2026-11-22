package dev.ledgerflow.account;

import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountLifecycleTest {

    @Autowired
    AccountService accounts;
    @Autowired
    LedgerService ledger;
    @Autowired
    MockMvc mvc;

    String cash;
    String wallet;
    UUID walletId;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accounts);
        cash = t.asset("cash");
        wallet = t.wallet("wallet");
        walletId = accounts.getByCode(wallet).getId();
    }

    @Test
    void frozenAccountRejectsPostingsUntilUnfrozen() {
        accounts.freeze(walletId);

        assertThatThrownBy(() -> fund(100))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("frozen");

        accounts.unfreeze(walletId);
        fund(100);
        assertThat(accounts.get(walletId).getBalanceMinor()).isEqualTo(100);
    }

    @Test
    void closeRequiresZeroBalance() {
        fund(100);

        assertThatThrownBy(() -> accounts.close(walletId)).hasMessageContaining("zero balance");
    }

    @Test
    void closedAccountIsFinal() throws Exception {
        mvc.perform(post("/api/v1/accounts/" + walletId + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mvc.perform(post("/api/v1/accounts/" + walletId + "/unfreeze"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("account_closed"));
        assertThatThrownBy(() -> fund(1)).hasMessageContaining("closed");
    }

    private void fund(long amount) {
        ledger.post(new PostEntryCommand("fund", null, "INR", List.of(debit(cash, amount), credit(wallet, amount))));
    }
}
