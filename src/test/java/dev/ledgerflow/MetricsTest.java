package dev.ledgerflow;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetricsTest {

    @Autowired
    MeterRegistry registry;
    @Autowired
    LedgerService ledger;
    @Autowired
    AccountService accounts;
    @Autowired
    MockMvc mvc;

    @Test
    void countsPostedEntries() {
        TestAccounts t = new TestAccounts(accounts);
        String cash = t.asset("cash");
        String wallet = t.wallet("wallet");
        double before = count();

        ledger.post(new PostEntryCommand("m", null, "INR", List.of(debit(cash, 1), credit(wallet, 1))));

        assertThat(count()).isEqualTo(before + 1);
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    private double count() {
        var counter = registry.find("ledger.entries").tag("type", "entry.posted").tag("currency", "INR").counter();
        return counter == null ? 0 : counter.count();
    }
}
