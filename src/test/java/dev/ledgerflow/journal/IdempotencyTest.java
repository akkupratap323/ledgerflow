package dev.ledgerflow.journal;

import dev.ledgerflow.account.AccountService;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    IdempotentPosting posting;
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
    void retryReturnsOriginalEntryAndPostsOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = JournalControllerTest.entry(cash, wallet, 1_000, 1_000);

        mvc.perform(post("/api/v1/entries").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"));

        mvc.perform(post("/api/v1/entries").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"));

        assertThat(accountService.getByCode(wallet).getBalanceMinor()).isEqualTo(1_000);
    }

    @Test
    void sameKeyDifferentPayloadIsConflict() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/api/v1/entries").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JournalControllerTest.entry(cash, wallet, 1_000, 1_000)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/entries").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JournalControllerTest.entry(cash, wallet, 9_000, 9_000)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
    }

    @Test
    void concurrentRetriesWithSameKeyPostOnlyOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        PostEntryCommand command = new PostEntryCommand("race", null, "INR",
                List.of(debit(cash, 500), credit(wallet, 500)));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<PostResult>> calls = IntStream.range(0, 8)
                    .<Callable<PostResult>>mapToObj(i -> () -> posting.post(command, key)).toList();
            List<UUID> ids = pool.invokeAll(calls).stream().map(IdempotencyTest::await)
                    .map(r -> r.entry().getId()).distinct().toList();

            assertThat(ids).hasSize(1);
            assertThat(accountService.getByCode(wallet).getBalanceMinor()).isEqualTo(500);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void serviceRejectsReusedKeyWithNewPayload() {
        String key = UUID.randomUUID().toString();
        posting.post(new PostEntryCommand("a", null, "INR", List.of(debit(cash, 1), credit(wallet, 1))), key);

        assertThatThrownBy(() -> posting.post(
                new PostEntryCommand("b", null, "INR", List.of(debit(cash, 1), credit(wallet, 1))), key))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void overlongIdempotencyKeyIs400() throws Exception {
        mvc.perform(post("/api/v1/entries").header("Idempotency-Key", "k".repeat(101))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JournalControllerTest.entry(cash, wallet, 1, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    private static PostResult await(Future<PostResult> f) {
        try {
            return f.get();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
