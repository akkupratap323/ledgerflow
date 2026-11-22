package dev.ledgerflow.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        Account account = service.open(request.code(), request.name(), request.type(),
                request.currency(), request.allowNegative());
        return AccountResponse.from(account);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(service.get(id));
    }

    @GetMapping("/by-code/{code}")
    public AccountResponse getByCode(@PathVariable String code) {
        return AccountResponse.from(service.getByCode(code));
    }

    @PostMapping("/{id}/freeze")
    public AccountResponse freeze(@PathVariable UUID id) {
        return AccountResponse.from(service.freeze(id));
    }

    @PostMapping("/{id}/unfreeze")
    public AccountResponse unfreeze(@PathVariable UUID id) {
        return AccountResponse.from(service.unfreeze(id));
    }

    @PostMapping("/{id}/close")
    public AccountResponse close(@PathVariable UUID id) {
        return AccountResponse.from(service.close(id));
    }

    @GetMapping
    public List<AccountResponse> list() {
        return service.list().stream().map(AccountResponse::from).toList();
    }
}
