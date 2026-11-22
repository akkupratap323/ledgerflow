package dev.ledgerflow.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    AccountRepository repository;

    @Test
    void savesAndFindsByCode() {
        repository.saveAndFlush(new Account("wallet:user_1", "User 1 wallet", AccountType.LIABILITY, "INR", false));

        assertThat(repository.findByCode("wallet:user_1"))
                .hasValueSatisfying(a -> {
                    assertThat(a.getType()).isEqualTo(AccountType.LIABILITY);
                    assertThat(a.getBalanceMinor()).isZero();
                });
        assertThat(repository.existsByCode("wallet:missing")).isFalse();
    }

    @Test
    void enforcesUniqueCode() {
        repository.saveAndFlush(new Account("cash:bank", "Bank", AccountType.ASSET, "INR", true));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new Account("cash:bank", "Duplicate", AccountType.ASSET, "INR", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
