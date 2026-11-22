package dev.ledgerflow.account;

import dev.ledgerflow.common.IsoCurrency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OpenAccountRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9_:.-]+$", message = "may only contain lowercase letters, digits and _ : . -")
        String code,

        @NotBlank @Size(max = 200) String name,

        @NotNull AccountType type,

        @NotNull @IsoCurrency String currency,

        boolean allowNegative
) {
}
