package dev.ledgerflow.hold;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/holds")
public class HoldController {

    private final HoldService holds;

    public HoldController(HoldService holds) {
        this.holds = holds;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse place(@Valid @RequestBody HoldRequests.Place request) {
        return HoldResponse.from(holds.place(request.account(), request.amountMinor(), request.reference(),
                Duration.ofSeconds(request.ttlSeconds())));
    }

    @PostMapping("/{id}/capture")
    public HoldResponse capture(@PathVariable UUID id, @Valid @RequestBody HoldRequests.Capture request) {
        return HoldResponse.from(holds.capture(id, request.amountMinor(), request.destination()));
    }

    @PostMapping("/{id}/void")
    public HoldResponse voidHold(@PathVariable UUID id) {
        return HoldResponse.from(holds.voidHold(id));
    }

    @GetMapping("/{id}")
    public HoldResponse get(@PathVariable UUID id) {
        return HoldResponse.from(holds.get(id));
    }
}
