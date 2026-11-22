package dev.ledgerflow.journal;

import dev.ledgerflow.common.Side;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(
        UUID id,
        String reference,
        String description,
        String currency,
        Instant createdAt,
        UUID reversalOf,
        List<Line> postings
) {
    public record Line(int lineNo, String account, Side side, long amountMinor) {
    }

    public static JournalEntryResponse from(JournalEntry e) {
        return new JournalEntryResponse(e.getId(), e.getReference(), e.getDescription(), e.getCurrency(),
                e.getCreatedAt(), e.getReversalOf(),
                e.getPostings().stream()
                        .map(p -> new Line(p.getLineNo(), p.getAccount().getCode(), p.getSide(), p.getAmountMinor()))
                        .toList());
    }
}
