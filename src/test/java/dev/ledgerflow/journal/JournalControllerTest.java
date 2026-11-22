package dev.ledgerflow.journal;

import com.jayway.jsonpath.JsonPath;
import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JournalControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    AccountService accountService;

    String cash;
    String wallet;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accountService);
        cash = t.asset("cash");
        wallet = t.wallet("wallet");
    }

    @Test
    void postsAndReadsBackAnEntry() throws Exception {
        String body = mvc.perform(post("/api/v1/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entry(cash, wallet, 2_500, 2_500)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postings.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(body, "$.id");
        mvc.perform(get("/api/v1/entries/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postings[0].account").value(cash))
                .andExpect(jsonPath("$.postings[1].side").value("CREDIT"));
    }

    @Test
    void unbalancedEntryIs422() throws Exception {
        mvc.perform(post("/api/v1/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entry(cash, wallet, 2_500, 2_000)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("unbalanced_entry"));
    }

    @Test
    void negativeAmountFailsValidation() throws Exception {
        mvc.perform(post("/api/v1/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entry(cash, wallet, -5, -5)))
                .andExpect(status().isBadRequest());
    }

    static String entry(String debitAccount, String creditAccount, long debit, long credit) {
        return """
                {"reference":"ref-1","currency":"INR","postings":[
                  {"account":"%s","side":"DEBIT","amountMinor":%d},
                  {"account":"%s","side":"CREDIT","amountMinor":%d}
                ]}
                """.formatted(debitAccount, debit, creditAccount, credit);
    }
}
