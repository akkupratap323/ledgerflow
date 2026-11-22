package dev.ledgerflow.transfer;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    AccountService accountService;
    @Autowired
    LedgerService ledger;

    String alice;
    String bob;
    String fees;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accountService);
        String cash = t.asset("cash");
        alice = t.wallet("alice");
        bob = t.wallet("bob");
        fees = t.revenue("fees");
        ledger.post(new PostEntryCommand("fund", null, "INR", List.of(debit(cash, 10_000), credit(alice, 10_000))));
    }

    @Test
    void transferWithFee() throws Exception {
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"from":"%s","to":"%s","amountMinor":2000,"currency":"INR","feeMinor":50,"feeAccount":"%s"}
                        """.formatted(alice, bob, fees)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postings.length()").value(3));

        assertThat(balance(alice)).isEqualTo(8_000);
        assertThat(balance(bob)).isEqualTo(1_950);
        assertThat(balance(fees)).isEqualTo(50);
    }

    @Test
    void feeWithoutFeeAccountIsRejected() throws Exception {
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"from":"%s","to":"%s","amountMinor":2000,"currency":"INR","feeMinor":50}
                        """.formatted(alice, bob)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.feeValid").exists());
    }

    @Test
    void selfTransferIsRejected() throws Exception {
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"from":"%s","to":"%s","amountMinor":2000,"currency":"INR"}
                        """.formatted(alice, alice)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientFundsIs422() throws Exception {
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"from":"%s","to":"%s","amountMinor":20000,"currency":"INR"}
                        """.formatted(alice, bob)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("insufficient_funds"));
    }

    private long balance(String code) {
        return accountService.getByCode(code).getBalanceMinor();
    }
}
