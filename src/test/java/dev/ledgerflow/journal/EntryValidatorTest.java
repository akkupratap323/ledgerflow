package dev.ledgerflow.journal;

import dev.ledgerflow.common.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntryValidatorTest {

    @Test
    void acceptsBalancedEntry() {
        assertThatCode(() -> EntryValidator.validate(List.of(
                debit("cash", 1_000), credit("wallet:a", 700), credit("revenue:fees", 300))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnbalancedEntry() {
        assertThatThrownBy(() -> EntryValidator.validate(List.of(debit("cash", 1_000), credit("wallet:a", 999))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must equal");
    }

    @Test
    void rejectsSinglePosting() {
        assertThatThrownBy(() -> EntryValidator.validate(List.of(debit("cash", 1_000))))
                .hasMessageContaining("at least two");
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> EntryValidator.validate(List.of(debit("cash", 0), credit("wallet:a", 0))))
                .hasMessageContaining("positive");
    }

    @Test
    void rejectsOneSidedEntry() {
        assertThatThrownBy(() -> EntryValidator.validate(List.of(debit("cash", 5), debit("fees", 5))))
                .hasMessageContaining("one debit and one credit");
    }
}
