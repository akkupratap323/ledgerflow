package dev.ledgerflow.journal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Handles the race where two requests with the same idempotency key arrive at once.
 * The loser's insert hits the unique constraint, and it gets the winner's entry back.
 */
@Component
public class IdempotentPosting {

    private final LedgerService ledger;

    public IdempotentPosting(LedgerService ledger) {
        this.ledger = ledger;
    }

    public PostResult post(PostEntryCommand command, String idempotencyKey) {
        try {
            return ledger.post(command, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey == null) {
                throw e;
            }
            return ledger.replay(command, idempotencyKey);
        }
    }
}
