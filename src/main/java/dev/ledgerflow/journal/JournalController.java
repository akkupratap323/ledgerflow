package dev.ledgerflow.journal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/entries")
public class JournalController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String REPLAYED = "Idempotent-Replayed";

    private final LedgerService ledger;
    private final IdempotentPosting idempotentPosting;

    public JournalController(LedgerService ledger, IdempotentPosting idempotentPosting) {
        this.ledger = ledger;
        this.idempotentPosting = idempotentPosting;
    }

    @PostMapping
    public ResponseEntity<JournalEntryResponse> post(
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody PostEntryRequest request) {
        PostResult result = idempotentPosting.post(request.toCommand(), idempotencyKey);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header(REPLAYED, String.valueOf(result.replayed()))
                .body(JournalEntryResponse.from(result.entry()));
    }

    @GetMapping("/{id}")
    public JournalEntryResponse get(@PathVariable UUID id) {
        return JournalEntryResponse.from(ledger.get(id));
    }
}
