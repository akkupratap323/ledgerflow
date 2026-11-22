package dev.ledgerflow.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdFilterTest {

    @Autowired
    MockMvc mvc;

    @Test
    void echoesCallerRequestId() throws Exception {
        mvc.perform(get("/api/v1/accounts").header("X-Request-Id", "req-123"))
                .andExpect(header().string("X-Request-Id", "req-123"));
    }

    @Test
    void replacesUnsafeOrMissingIds() throws Exception {
        String generated = mvc.perform(get("/api/v1/accounts").header("X-Request-Id", "bad id\r\ninjected"))
                .andReturn().getResponse().getHeader("X-Request-Id");

        assertThat(generated).hasSize(36).doesNotContain(" ");
    }
}
