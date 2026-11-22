package dev.ledgerflow.journal;

import dev.ledgerflow.common.IsoCurrency;
import dev.ledgerflow.common.Side;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostEntryRequest(
        @Size(max = 100) String reference,
        @Size(max = 500) String description,
        @NotNull @IsoCurrency String currency,
        @NotEmpty @Size(max = 50) List<@Valid Line> postings
) {
    public record Line(
            @NotBlank String account,
            @NotNull Side side,
            @Positive long amountMinor
    ) {
    }

    public PostEntryCommand toCommand() {
        return new PostEntryCommand(reference, description, currency,
                postings.stream().map(l -> new PostingLine(l.account(), l.side(), l.amountMinor())).toList());
    }
}
