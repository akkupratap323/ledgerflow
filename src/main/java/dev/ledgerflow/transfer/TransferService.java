package dev.ledgerflow.transfer;

import dev.ledgerflow.journal.IdempotentPosting;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.journal.PostResult;
import dev.ledgerflow.journal.PostingLine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Turns a simple "send money" request into a balanced journal entry. */
@Service
public class TransferService {

    private final IdempotentPosting posting;

    public TransferService(IdempotentPosting posting) {
        this.posting = posting;
    }

    public PostResult transfer(TransferRequest request, String idempotencyKey) {
        List<PostingLine> lines = new ArrayList<>();
        lines.add(PostingLine.debit(request.from(), request.amountMinor()));
        lines.add(PostingLine.credit(request.to(), request.amountMinor() - request.feeMinor()));
        if (request.feeMinor() > 0) {
            lines.add(PostingLine.credit(request.feeAccount(), request.feeMinor()));
        }
        String description = request.description() != null ? request.description()
                : "Transfer from " + request.from() + " to " + request.to();
        return posting.post(new PostEntryCommand(request.reference(), description, request.currency(), lines),
                idempotencyKey);
    }
}
