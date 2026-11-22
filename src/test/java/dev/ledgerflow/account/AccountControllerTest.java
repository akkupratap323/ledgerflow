package dev.ledgerflow.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void opensAndFetchesAccount() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"wallet:carol","name":"Carol","type":"LIABILITY","currency":"INR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("wallet:carol"))
                .andExpect(jsonPath("$.balance").value("INR 0.00"));

        mvc.perform(get("/api/v1/accounts/by-code/wallet:carol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("LIABILITY"));
    }

    @Test
    void returnsFieldErrorsForInvalidRequest() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"Bad Code!","name":"","type":"LIABILITY","currency":"RUPEE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fields.code").exists())
                .andExpect(jsonPath("$.fields.currency").exists());
    }

    @Test
    void duplicateCodeIsConflict() throws Exception {
        String body = """
                {"code":"cash:bank","name":"Bank","type":"ASSET","currency":"INR","allowNegative":true}
                """;
        mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("account_exists"));
    }

    @Test
    void unknownAccountIs404() throws Exception {
        mvc.perform(get("/api/v1/accounts/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }
}
