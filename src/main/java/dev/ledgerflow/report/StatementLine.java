package dev.ledgerflow.report;

import dev.ledgerflow.common.Side;

import java.time.Instant;
import java.util.UUID;

public record StatementLine(
        long seq,
        UUID entryId,
        Instant postedAt,
        String reference,
        String description,
        Side side,
        long amountMinor,
        long balanceAfterMinor
) {
}
