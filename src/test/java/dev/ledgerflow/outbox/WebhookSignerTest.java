package dev.ledgerflow.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookSignerTest {

    private final WebhookSigner signer = new WebhookSigner("whsec_test_0123456789");

    @Test
    void signatureIsDeterministicAndPrefixed() {
        String a = signer.sign(1_700_000_000L, "{\"id\":1}");

        assertThat(a).startsWith("sha256=").hasSize(7 + 64);
        assertThat(signer.sign(1_700_000_000L, "{\"id\":1}")).isEqualTo(a);
    }

    @Test
    void timestampAndBodyBothAffectTheSignature() {
        String base = signer.sign(1L, "body");

        assertThat(signer.sign(2L, "body")).isNotEqualTo(base);
        assertThat(signer.sign(1L, "body!")).isNotEqualTo(base);
    }

    @Test
    void rejectsWeakSecret() {
        assertThatThrownBy(() -> new WebhookSigner("short")).isInstanceOf(IllegalArgumentException.class);
    }
}
