package dev.ledgerflow.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void addsAndSubtractsInMinorUnits() {
        Money a = Money.of(10_050, "INR");
        Money b = Money.of(2_025, "inr");

        assertThat(a.plus(b)).isEqualTo(Money.of(12_075, "INR"));
        assertThat(a.minus(b)).isEqualTo(Money.of(8_025, "INR"));
    }

    @Test
    void rejectsMixedCurrencies() {
        assertThatThrownBy(() -> Money.of(100, "INR").plus(Money.of(100, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void detectsOverflow() {
        assertThatThrownBy(() -> Money.of(Long.MAX_VALUE, "USD").plus(Money.of(1, "USD")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void formatsUsingCurrencyFractionDigits() {
        assertThat(Money.of(12_050, "INR").format()).isEqualTo("INR 120.50");
        assertThat(Money.of(500, "JPY").format()).isEqualTo("JPY 500");
    }

    @Test
    void signHelpers() {
        assertThat(Money.of(-1, "USD").isNegative()).isTrue();
        assertThat(Money.zero("USD").isZero()).isTrue();
        assertThat(Money.of(5, "USD").negate()).isEqualTo(Money.of(-5, "USD"));
    }
}
