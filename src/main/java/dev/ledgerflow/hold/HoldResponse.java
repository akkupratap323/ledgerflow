package dev.ledgerflow.hold;

import java.time.Instant;
import java.util.UUID;

public record HoldResponse(
        UUID id,
        UUID accountId,
        long amountMinor,
        String currency,
        HoldStatus status,
        String reference,
        Instant expiresAt,
        Instant createdAt,
        Instant resolvedAt,
        UUID captureEntryId
) {
    public static HoldResponse from(Hold h) {
        return new HoldResponse(h.getId(), h.getAccountId(), h.getAmountMinor(), h.getCurrency(), h.getStatus(),
                h.getReference(), h.getExpiresAt(), h.getCreatedAt(), h.getResolvedAt(), h.getCaptureEntryId());
    }
}
