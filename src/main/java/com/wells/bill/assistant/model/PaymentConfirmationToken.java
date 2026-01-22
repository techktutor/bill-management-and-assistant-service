package com.wells.bill.assistant.model;

import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmationToken(
        String token,
        UUID billId,
        UUID userId,
        Instant expiresAt
) {}

