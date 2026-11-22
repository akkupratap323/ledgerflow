package dev.ledgerflow.outbox;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Signs webhook bodies with HMAC-SHA256 over "timestamp.body", the same scheme
 * payment providers use. Receivers recompute it and reject stale timestamps.
 */
public final class WebhookSigner {

    private final byte[] secret;

    public WebhookSigner(String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("Webhook secret must be at least 16 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(long timestampSeconds, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal((timestampSeconds + "." + body).getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
