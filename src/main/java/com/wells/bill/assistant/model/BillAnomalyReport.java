package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillAnomalyReport(

        UUID billId,

        boolean anomalous,
        int anomalyScore, // 0–100

        BigDecimal billAmount,
        BigDecimal averageAmount,

        List<String> detectedSignals,
        String summary
) {
}

