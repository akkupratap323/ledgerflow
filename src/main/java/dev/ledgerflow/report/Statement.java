package dev.ledgerflow.report;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Statement(
        UUID accountId,
        String accountCode,
        String currency,
        Instant from,
        Instant to,
        int page,
        int size,
        long totalLines,
        List<StatementLine> lines
) {
}
