package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record SpendingTrendReport(

        UUID userId,

        YearMonth currentMonth,
        BigDecimal currentMonthSpend,

        YearMonth previousMonth,
        BigDecimal previousMonthSpend,

        BigDecimal percentageChange,

        String trend, // INCREASING / DECREASING / STABLE

        List<String> signals,
        String summary
) {
}

