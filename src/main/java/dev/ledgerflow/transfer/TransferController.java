package dev.ledgerflow.transfer;

import dev.ledgerflow.journal.JournalEntryResponse;
import dev.ledgerflow.journal.PostResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    public ResponseEntity<JournalEntryResponse> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        PostResult result = transfers.transfer(request, idempotencyKey);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(JournalEntryResponse.from(result.entry()));
    }
}
