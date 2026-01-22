package com.wells.bill.assistant.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

public final class IdempotencyKeyGenerator {

    private IdempotencyKeyGenerator() {
    }

    public static String generate(
            UUID userId,
            UUID billId,
            BigDecimal amount,
            String currency
    ) {
        try {
            String canonical = canonicalize(userId, billId, amount, currency);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    canonical.getBytes(StandardCharsets.UTF_8)
            );

            // URL-safe, header-safe, compact
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate idempotency key", ex);
        }
    }

    private static String canonicalize(
            UUID userId,
            UUID billId,
            BigDecimal amount,
            String currency
    ) {
        return String.join("|",
                userId.toString(),
                billId.toString(),
                normalizeAmount(amount),
                currency.toUpperCase()
        );
    }

    /**
     * Ensures 100.0 == 100.00 == 100.000
     */
    private static String normalizeAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}

