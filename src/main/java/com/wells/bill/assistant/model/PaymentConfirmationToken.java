package com.wells.bill.assistant.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentConfirmationToken(
        String token,
        UUID billId,
        UUID userId,
        LocalDate scheduledDate,
        Instant expiresAt
) {}

