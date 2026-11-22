package dev.ledgerflow.journal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;

class RequestHasherTest {

    @Test
    void sameRequestSameHash() {
        PostEntryCommand a = new PostEntryCommand("r", "d", "inr", List.of(debit("x", 1), credit("y", 1)));
        PostEntryCommand b = new PostEntryCommand("r", "d", "INR", List.of(debit("x", 1), credit("y", 1)));

        assertThat(RequestHasher.hash(a)).isEqualTo(RequestHasher.hash(b)).hasSize(64);
    }

    @Test
    void differentAmountDifferentHash() {
        PostEntryCommand a = new PostEntryCommand("r", null, "INR", List.of(debit("x", 1), credit("y", 1)));
        PostEntryCommand b = new PostEntryCommand("r", null, "INR", List.of(debit("x", 2), credit("y", 2)));

        assertThat(RequestHasher.hash(a)).isNotEqualTo(RequestHasher.hash(b));
    }
}
