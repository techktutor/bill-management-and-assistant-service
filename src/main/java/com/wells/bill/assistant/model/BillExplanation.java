package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BillExplanation(

        UUID billId,

        String provider,
        String consumer,
        String serviceNumber,

        BigDecimal amountDue,
        String currency,

        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        LocalDate dueDate,

        String currentStatus,

        boolean isOverdue,
        boolean isPayable,

        String plainEnglishSummary
) {
}
