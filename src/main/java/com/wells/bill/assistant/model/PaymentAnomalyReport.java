package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentAnomalyReport(

        UUID paymentId,
        boolean anomalous,
        int anomalyScore, // 0–100

        BigDecimal amount,
        BigDecimal averageAmount,

        List<String> signals,
        String summary
) {
}

