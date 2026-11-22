package dev.ledgerflow.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IsoCurrencyTest {

    private final IsoCurrency.Validator validator = new IsoCurrency.Validator();

    @ParameterizedTest
    @ValueSource(strings = {"INR", "USD", "eur", "JPY"})
    void acceptsIsoCodes(String code) {
        assertThat(validator.isValid(code, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "RUPEE", "XYZ", "US"})
    void rejectsUnknownCodes(String code) {
        assertThat(validator.isValid(code, null)).isFalse();
    }
}
