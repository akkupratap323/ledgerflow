package dev.ledgerflow.journal;

import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.common.Side;

import java.util.List;

/** Enforces the double-entry rule: total debits equal total credits. */
public final class EntryValidator {

    private EntryValidator() {
    }

    public static void validate(List<PostingLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new BusinessRuleException("too_few_postings", "An entry needs at least two postings");
        }
        long debits = 0;
        long credits = 0;
        for (PostingLine line : lines) {
            if (line.amountMinor() <= 0) {
                throw new BusinessRuleException("invalid_amount",
                        "Posting amounts must be positive, got " + line.amountMinor());
            }
            if (line.side() == Side.DEBIT) {
                debits = Math.addExact(debits, line.amountMinor());
            } else {
                credits = Math.addExact(credits, line.amountMinor());
            }
        }
        if (debits == 0 || credits == 0) {
            throw new BusinessRuleException("unbalanced_entry", "An entry needs at least one debit and one credit");
        }
        if (debits != credits) {
            throw new BusinessRuleException("unbalanced_entry",
                    "Debits (" + debits + ") must equal credits (" + credits + ")");
        }
    }
}
