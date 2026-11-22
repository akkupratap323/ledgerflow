package dev.ledgerflow.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.journal.JournalEntry;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ledgerflow.outbox.max-attempts=3",
        "ledgerflow.outbox.batch-size=10000",
        "ledgerflow.outbox.base-backoff=PT10S"
})
class OutboxRelayTest {

    /** Fails a configurable number of times per aggregate, then succeeds. */
    static class FlakyPublisher implements EventPublisher {
        final Map<UUID, Integer> failuresLeft = new ConcurrentHashMap<>();
        final List<OutboxEvent> delivered = new CopyOnWriteArrayList<>();

        @Override
        public void publish(OutboxEvent event) {
            Integer left = failuresLeft.computeIfPresent(event.getAggregateId(), (k, v) -> v - 1);
            if (left != null && left >= 0) {
                throw new IllegalStateException("receiver down");
            }
            delivered.add(event);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FlakyPublisher flakyPublisher() {
            return new FlakyPublisher();
        }
    }

    @Autowired
    OutboxRelay relay;
    @Autowired
    OutboxRepository outbox;
    @Autowired
    FlakyPublisher publisher;
    @Autowired
    LedgerService ledger;
    @Autowired
    AccountService accounts;
    @Autowired
    ObjectMapper mapper;

    String cash;
    String wallet;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accounts);
        cash = t.asset("cash");
        wallet = t.wallet("wallet");
    }

    @Test
    void postingWritesAnEventWithTheEntryAsPayload() throws Exception {
        JournalEntry entry = post();

        OutboxEvent event = single(entry.getId());
        assertThat(event.getEventType()).isEqualTo("entry.posted");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(mapper.readTree(event.getPayload()).get("id").asText()).isEqualTo(entry.getId().toString());
    }

    @Test
    void failedDeliveryIsRetriedWithBackoffThenSent() {
        JournalEntry entry = post();
        publisher.failuresLeft.put(entry.getId(), 2);
        Instant t0 = Instant.now().truncatedTo(ChronoUnit.MICROS); // database timestamp precision

        relay.relayDue(t0);
        OutboxEvent afterFirst = single(entry.getId());
        assertThat(afterFirst.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(afterFirst.getAttempts()).isEqualTo(1);
        assertThat(afterFirst.getLastError()).contains("receiver down");
        assertThat(afterFirst.getNextAttemptAt()).isEqualTo(t0.plusSeconds(10));

        relay.relayDue(t0.plusSeconds(1)); // not due yet
        assertThat(single(entry.getId()).getAttempts()).isEqualTo(1);

        relay.relayDue(t0.plus(Duration.ofMinutes(1)));
        assertThat(single(entry.getId()).getAttempts()).isEqualTo(2);

        relay.relayDue(t0.plus(Duration.ofHours(1)));
        OutboxEvent done = single(entry.getId());
        assertThat(done.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(done.getAttempts()).isEqualTo(3);
        assertThat(publisher.delivered).anyMatch(e -> e.getAggregateId().equals(entry.getId()));
    }

    @Test
    void eventIsDeadLetteredAfterMaxAttempts() {
        JournalEntry entry = post();
        publisher.failuresLeft.put(entry.getId(), 100);
        Instant t = Instant.now();

        for (int i = 0; i < 5; i++) {
            relay.relayDue(t.plus(Duration.ofHours(i)));
        }

        OutboxEvent dead = single(entry.getId());
        assertThat(dead.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(dead.getAttempts()).isEqualTo(3);
    }

    @Test
    void backoffDoublesAndIsCapped() {
        OutboxProperties props = new OutboxProperties(null, null, null, null,
                Duration.ofSeconds(5), Duration.ofMinutes(1));

        assertThat(props.backoffFor(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.backoffFor(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.backoffFor(4)).isEqualTo(Duration.ofSeconds(40));
        assertThat(props.backoffFor(10)).isEqualTo(Duration.ofMinutes(1));
    }

    private JournalEntry post() {
        return ledger.post(new PostEntryCommand("evt", null, "INR", List.of(debit(cash, 10), credit(wallet, 10))));
    }

    private OutboxEvent single(UUID aggregateId) {
        List<OutboxEvent> events = outbox.findByAggregateId(aggregateId);
        assertThat(events).hasSize(1);
        return events.get(0);
    }
}
