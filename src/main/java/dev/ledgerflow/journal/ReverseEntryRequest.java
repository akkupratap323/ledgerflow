package dev.ledgerflow.journal;

import jakarta.validation.constraints.Size;

public record ReverseEntryRequest(@Size(max = 300) String reason) {
}
