package dev.ledgerflow.account;

import dev.ledgerflow.common.ConflictException;
import dev.ledgerflow.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AccountServiceTest {

    @Autowired
    AccountService service;

    @Test
    void opensAccountWithZeroBalance() {
        Account account = service.open("wallet:alice", "Alice", AccountType.LIABILITY, "inr", false);

        assertThat(account.getCurrency()).isEqualTo("INR");
        assertThat(service.getByCode("wallet:alice").getBalanceMinor()).isZero();
    }

    @Test
    void rejectsDuplicateCode() {
        service.open("wallet:bob", "Bob", AccountType.LIABILITY, "INR", false);

        assertThatThrownBy(() -> service.open("wallet:bob", "Bob again", AccountType.LIABILITY, "INR", false))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownAccountIsNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }
}
