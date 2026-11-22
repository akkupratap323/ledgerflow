package dev.ledgerflow.journal;

import java.util.List;

public record PostEntryCommand(
        String reference,
        String description,
        String currency,
        List<PostingLine> lines
) {
}
