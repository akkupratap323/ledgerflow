package dev.ledgerflow.transfer;

import dev.ledgerflow.common.IsoCurrency;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Move money from one account to another, optionally taking a fee.
 * The sender is charged {@code amountMinor}; the receiver gets {@code amountMinor - feeMinor}.
 */
public record TransferRequest(
        @NotBlank String from,
        @NotBlank String to,
        @Positive long amountMinor,
        @NotNull @IsoCurrency String currency,
        @PositiveOrZero long feeMinor,
        String feeAccount,
        @Size(max = 100) String reference,
        @Size(max = 500) String description
) {
    @AssertTrue(message = "feeAccount is required when feeMinor is set, and the fee must be below the amount")
    public boolean isFeeValid() {
        if (feeMinor == 0) {
            return true;
        }
        return feeAccount != null && !feeAccount.isBlank() && feeMinor < amountMinor;
    }

    @AssertTrue(message = "from and to must be different accounts")
    public boolean isDistinctAccounts() {
        return from == null || !from.equals(to);
    }
}
