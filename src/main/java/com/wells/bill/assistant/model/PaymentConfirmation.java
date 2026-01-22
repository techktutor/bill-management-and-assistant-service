package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentConfirmation(

        UUID billId,
        UUID userId,

        BigDecimal amount,
        String currency,

        LocalDate scheduledDate, // null = immediate

        String confirmationToken,
        String message
) {
}
