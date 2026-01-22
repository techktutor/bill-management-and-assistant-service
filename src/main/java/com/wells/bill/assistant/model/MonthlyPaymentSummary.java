package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record MonthlyPaymentSummary(

        UUID userId,
        YearMonth month,

        int totalPayments,
        BigDecimal totalAmount,

        BigDecimal successfulAmount,
        BigDecimal failedAmount,
        BigDecimal scheduledAmount,

        List<String> categoryBreakdown,
        String summary
) {}

