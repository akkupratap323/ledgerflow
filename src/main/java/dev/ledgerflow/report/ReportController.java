package dev.ledgerflow.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final StatementService statements;

    public ReportController(StatementService statements) {
        this.statements = statements;
    }

    @GetMapping("/accounts/{id}/statement")
    public Statement statement(@PathVariable UUID id,
                               @RequestParam(required = false) Instant from,
                               @RequestParam(required = false) Instant to,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "100") int size) {
        return statements.statement(id, from, to, page, size);
    }
}
