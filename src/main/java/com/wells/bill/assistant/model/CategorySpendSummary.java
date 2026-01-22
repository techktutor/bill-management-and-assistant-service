package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

public record CategorySpendSummary(

        UUID userId,
        YearMonth month,

        Map<String, BigDecimal> categoryTotals,
        String summary
) {}

