package dev.ledgerflow.journal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fingerprints a posting request so a reused idempotency key with a different
 * payload can be told apart from an honest retry.
 */
public final class RequestHasher {

    private RequestHasher() {
    }

    public static String hash(PostEntryCommand command) {
        StringBuilder canonical = new StringBuilder()
                .append(nullToEmpty(command.reference())).append('|')
                .append(nullToEmpty(command.description())).append('|')
                .append(command.currency().toUpperCase());
        for (PostingLine line : command.lines()) {
            canonical.append('|').append(line.accountCode())
                    .append(':').append(line.side())
                    .append(':').append(line.amountMinor());
        }
        return sha256(canonical.toString());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
