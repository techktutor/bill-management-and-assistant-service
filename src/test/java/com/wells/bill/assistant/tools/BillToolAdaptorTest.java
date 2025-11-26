package com.wells.bill.assistant.tools;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BillToolAdaptorTest {

    private final BillToolAdaptor tool = new BillToolAdaptor();

    @Test
    void classifyBill_electricityDetected() {
        String text = "Electricity bill - Meter reading: 12345 kWh of consumption";
        String type = tool.classifyBill(text);
        assertThat(type).isEqualTo("electricity");
    }

    @Test
    void extract_parsesAmountAndDueDate() {
        String text = "Total: $1,234.56\nDue Date: 12/02/2024\nBilling Period: 01/01/2024 - 01/31/2024";
        Map<String, Object> fields = tool.extract(text);

        assertThat(fields).isNotEmpty();
        assertThat(fields.get("totalAmount")).isEqualTo("1234.56");
        assertThat(fields.get("dueDate")).isNotNull();
        assertThat(fields.get("billingPeriod")).isNotNull();
    }

    @Test
    void detectAnomaly_detectsLargeIncrease() {
        List<Double> amounts = List.of(100.0, 150.0, 400.0);
        String result = tool.detectAnomaly(amounts);
        assertThat(result).contains("Anomaly detected");
    }

    @Test
    void trendAnalysis_detectsIncreasingDecreasingStable() {
        String inc = tool.trend(List.of(100.0, 110.0, 200.0));
        assertThat(inc).contains("increasing");

        String dec = tool.trend(List.of(200.0, 180.0, 150.0));
        assertThat(dec).contains("decreasing");

        String stable = tool.trend(List.of(100.0, 105.0, 108.0));
        assertThat(stable).contains("stable");
    }

    @Test
    void summarize_returnsStructuredSummary() {
        String text = "Company: Acme Corp\nTotal: $75.00\nDue Date: 2024-03-01";
        String summary = tool.summarize(text);
        assertThat(summary).contains("Bill Summary");
        assertThat(summary).contains("Amount");
    }
}
