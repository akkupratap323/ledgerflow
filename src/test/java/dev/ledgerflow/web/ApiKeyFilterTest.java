package dev.ledgerflow.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "ledgerflow.security.api-keys=key-one,key-two")
@AutoConfigureMockMvc
class ApiKeyFilterTest {

    @Autowired
    MockMvc mvc;

    @Test
    void missingKeyIsRejected() throws Exception {
        mvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void wrongKeyIsRejected() throws Exception {
        mvc.perform(get("/api/v1/accounts").header("X-API-Key", "key-three"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyConfiguredKeyIsAccepted() throws Exception {
        mvc.perform(get("/api/v1/accounts").header("X-API-Key", "key-one")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts").header("X-API-Key", "key-two")).andExpect(status().isOk());
    }
}
