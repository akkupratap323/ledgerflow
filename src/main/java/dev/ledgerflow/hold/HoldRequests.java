package dev.ledgerflow.hold;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class HoldRequests {

    private HoldRequests() {
    }

    public record Place(
            @NotBlank String account,
            @Positive long amountMinor,
            @Size(max = 100) String reference,
            @Positive @Max(2_592_000) long ttlSeconds
    ) {
    }

    public record Capture(
            @Positive long amountMinor,
            @NotBlank String destination
    ) {
    }
}
