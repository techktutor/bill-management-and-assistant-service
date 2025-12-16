package com.wells.bill.assistant.service;

import com.wells.bill.assistant.builder.FilterExpressionBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilterExpressionBuilderTest {

    @Test
    void eq_format_exact() {
        String expr = FilterExpressionBuilder
                .start()
                .eq("vendor", "ACME")
                .build();

        assertEquals("vendor = 'ACME'", expr);
    }

    @Test
    void in_format_exact() {
        String expr = FilterExpressionBuilder
                .start()
                .in("vendor", List.of("ACME", "OTHER"))
                .build();

        assertEquals("vendor IN ('ACME', 'OTHER')", expr);
    }

    @Test
    void and_or_exact_format() {
        String expr = FilterExpressionBuilder
                .start()
                .or(FilterExpressionBuilder
                        .start()
                        .eq("vendor", "ACME"),
                        FilterExpressionBuilder
                                .start()
                                .eq("vendor", "OTHER"))
                .and(FilterExpressionBuilder
                        .start()
                        .gte("amount", 100),
                        FilterExpressionBuilder
                                .start()
                                .lte("amount", 500))
                .build();

        assertEquals("(vendor = 'ACME' OR vendor = 'OTHER') (amount >= 100 AND amount <= 500)", expr);
    }
}
