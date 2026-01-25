package com.wells.bill.assistant.service;

import com.wells.bill.assistant.model.BillCategory;
import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.BillParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * End-to-end tests for BillParser
 * Covers:
 * - Rule-based parsing
 * - Hybrid merge (Rule + LLM)
 * - LLM-only fallback
 */
class BillParserTest {

    private ChatClient chatClient;
    private BillParser billParser;

    @BeforeEach
    void setup() {
        chatClient = Mockito.mock(ChatClient.class);
        billParser = new BillParser(chatClient);
    }

    /* -------------------------------------------------
     * RULE-BASED ONLY TESTS
     * ------------------------------------------------- */

    @Test
    void shouldParseElectricityBill_usingRulesOnly() {

        String bill = """
                TATA POWER ELECTRICITY BILL
                CONSUMER NAME: RAHUL SHARMA
                CONSUMER NUMBER: TP-88392011
                BILLING PERIOD: JAN 1 2026 - JAN 31 2026
                TOTAL AMOUNT DUE: ₹1,845.50
                DUE DATE: 15-FEB-2026
                VENDOR: TATA POWER
                CATEGORY: ELECTRICITY
                """;

        BillParseResult result = billParser.parse(bill);
        BillDetail detail = result.bill();

        assertEquals(new BigDecimal("1845.50"), detail.amountDue().amount());
        assertEquals("INR", detail.amountDue().currency().getCurrencyCode());
        assertEquals(LocalDate.of(2026, 2, 15), detail.dueDate());

        assertEquals(LocalDate.of(2026, 1, 1), detail.billingPeriod().start());
        assertEquals(LocalDate.of(2026, 1, 31), detail.billingPeriod().end());

        assertEquals("RAHUL SHARMA", detail.consumerName());
        assertEquals("TP-88392011", detail.consumerNumber());
        assertEquals(BillCategory.ELECTRICITY, detail.billCategory());

        assertTrue(result.overallConfidence() >= 85);
    }

    @Test
    void shouldHandleSingleLineInternetBill_usingRulesOnly() {

        String bill = """
                NETFIBRE BROADBAND INVOICE CONSUMER NAME: DURGESH RAI
                CONSUMER NUMBER: NF-90233491 BILLING PERIOD: MARCH 15, 2026 - APRIL 14, 2026
                PLAN: 200 MBPS UNLIMITED AMOUNT DUE: $59.14
                DUE DATE: APRIL 20, 2026 VENDOR: NETFIBRE BROADBAND
                """;

        BillParseResult result = billParser.parse(bill);
        BillDetail detail = result.bill();

        assertEquals("DURGESH RAI", detail.consumerName());
        assertEquals("NF-90233491", detail.consumerNumber());
        assertEquals(new BigDecimal("59.14"), detail.amountDue().amount());
        assertEquals("USD", detail.amountDue().currency().getCurrencyCode());
        assertEquals(LocalDate.of(2026, 4, 20), detail.dueDate());

        assertEquals(LocalDate.of(2026, 3, 15), detail.billingPeriod().start());
        assertEquals(LocalDate.of(2026, 4, 14), detail.billingPeriod().end());

        assertTrue(result.overallConfidence() >= 80);
    }

    /* -------------------------------------------------
     * HYBRID MERGE (RULE + LLM)
     * ------------------------------------------------- */

    @Test
    void shouldUseHybridMerge_whenRuleConfidenceIsLow() {

        String bill = """
                AIRTEL MOBILE BILL
                ACCOUNT NO: 99887766
                TOTAL AMOUNT DUE: ₹699
                DUE DATE: 10-MAR-2026
                """;

        // Stub LLM response
        Mockito.when(chatClient.prompt(anyString()).call().content())
                .thenReturn("""
                            {
                              "amountDue": { "amount": 699, "currency": "INR" },
                              "dueDate": "2026-03-10",
                              "providerName": "Airtel",
                              "billCategory": "MOBILE"
                            }
                        """);

        BillParseResult result = billParser.parse(bill);
        BillDetail detail = result.bill();

        // Rules must win for mandatory fields
        assertEquals(new BigDecimal("699"), detail.amountDue().amount());
        assertEquals(LocalDate.of(2026, 3, 10), detail.dueDate());

        // LLM may enrich
        assertNotNull(detail.billCategory());
        assertTrue(result.overallConfidence() >= 55);
    }

    /* -------------------------------------------------
     * LLM-ONLY FALLBACK
     * ------------------------------------------------- */

    @Test
    void shouldFallBackToLLM_whenMandatoryFieldsMissing() {

        String bill = """
                Hello,
                Your broadband invoice for April is ready.
                Please ensure payment by April 20, 2026.
                """;

        Mockito.when(chatClient.prompt(anyString()).call().content())
                .thenReturn("""
                            {
                              "amountDue": { "amount": 59.14, "currency": "$" },
                              "dueDate": "2026-04-20",
                              "billingPeriod": {
                                "startDate": "2026-03-15",
                                "endDate": "2026-04-14"
                              },
                              "consumerName": "Durgesh Rai",
                              "consumerNumber": "NF-90233491",
                              "providerName": "NetFibre Broadband",
                              "billCategory": "INTERNET"
                            }
                        """);

        BillParseResult result = billParser.parse(bill);
        BillDetail detail = result.bill();

        assertEquals(new BigDecimal("59.14"), detail.amountDue().amount());
        assertEquals("USD", detail.amountDue().currency().getCurrencyCode());
        assertEquals(LocalDate.of(2026, 4, 20), detail.dueDate());

        assertEquals(LocalDate.of(2026, 3, 15), detail.billingPeriod().start());
        assertEquals(LocalDate.of(2026, 4, 14), detail.billingPeriod().end());

        assertTrue(result.overallConfidence() >= 35);
    }

    /* -------------------------------------------------
     * REGRESSION TESTS
     * ------------------------------------------------- */

    @Test
    void shouldPreferTotalAmountOverSubtotal() {

        String bill = """
                UTILITY BILL
                SUBTOTAL: $120.00
                TAX: $10.00
                TOTAL AMOUNT DUE: $130.00
                DUE DATE: 25-MAR-2026
                """;

        BillParseResult result = billParser.parse(bill);

        assertEquals(
                new BigDecimal("130.00"),
                result.bill().amountDue().amount()
        );
    }

    @Test
    void shouldFailRuleConfidence_forInvalidDate() {

        String bill = """
                POWER BILL
                TOTAL AMOUNT DUE: ₹900
                DUE DATE: 31-FEB-2026
                """;

        BillParseResult result = billParser.parse(bill);

        assertEquals(0, result.overallConfidence());
    }
}
