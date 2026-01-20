package com.wells.bill.assistant.util;

import org.springframework.ai.document.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BillChunker {

    // ============================================================
    // Tuned regexes (real-world bill formats)
    // ============================================================

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)" +
                    "(total\\s+due|total\\s+amount|amount\\s+due|" +
                    "amount\\s+payable|balance\\s+due|net\\s+payable)" +
                    "\\D{0,30}" +
                    "(" +
                    "(?:₹|rs\\.?|inr|usd|\\$)?\\s*" +
                    "\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?" +
                    "\\s*(?:usd|inr)?" +
                    ")"
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)" +
                    "(due\\s+date|payment\\s+due|due\\s+on|bill\\s+date)" +
                    "\\W{0,20}" +
                    "(" +
                    "\\d{4}-\\d{2}-\\d{2}|" +           // ISO
                    "\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|" +
                    "\\d{1,2}\\s+[a-z]{3,9}\\s+\\d{4}"  // 10 Feb 2024
                    + ")"
    );

    // ============================================================
    // Public entry point
    // ============================================================

    public List<Document> chunkBillText(
            String text,
            String billId,
            String customerId,
            String filename
    ) {
        List<Document> docs = new ArrayList<>();
        Instant now = Instant.now();
        int idx = 0;

        String summary = extractSummarySection(text);
        if (!summary.isBlank()) {
            docs.add(doc(
                    summary,
                    billId,
                    customerId,
                    idx++,
                    "SUMMARY",
                    now,
                    filename
            ));
        }

        String amountSection = extractAmountSection(text);
        if (!amountSection.isBlank()) {
            docs.add(doc(
                    amountSection,
                    billId,
                    customerId,
                    idx++,
                    "AMOUNT",
                    now,
                    filename
            ));
        }

        String dateSection = extractDateSection(text);
        if (!dateSection.isBlank()) {
            docs.add(doc(
                    dateSection,
                    billId,
                    customerId,
                    idx++,
                    "DATES",
                    now,
                    filename
            ));
        }

        String lineItems = extractLineItems(text);
        if (!lineItems.isBlank()) {
            docs.add(doc(
                    lineItems,
                    billId,
                    customerId,
                    idx++,
                    "LINE_ITEMS",
                    now,
                    filename
            ));
        }

        return docs;
    }

    // ============================================================
    // Document factory
    // ============================================================

    private Document doc(
            String text,
            String billId,
            String customerId,
            int idx,
            String type,
            Instant now,
            String filename
    ) {
        return new Document(
                text,
                Map.of(
                        "bill_id", billId,
                        "customer_id", customerId,
                        "chunk_index", idx,
                        "chunk_type", type,
                        "ingested_at", now.toString(),
                        "source", filename
                )
        );
    }

    private String extractSummarySection(String text) {
        if (text == null) return "";

        String amount = extractAmountSection(text);
        String date = extractDateSection(text);

        if (amount.isBlank() && date.isBlank()) return "";

        return """
            Bill Summary:
            %s
            %s
            """.formatted(amount, date).trim();
    }

    // ============================================================
    // Amount extraction
    // ============================================================

    private String extractAmountSection(String text) {
        if (text == null) return "";

        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (!m.find()) return "";

        return """
                Amount Due: %s
                """.formatted(m.group(2).trim()).trim();
    }

    // ============================================================
    // Date extraction
    // ============================================================

    private String extractDateSection(String text) {
        if (text == null) return "";

        Matcher m = DATE_PATTERN.matcher(text);
        if (!m.find()) return "";

        return """
                Due Date: %s
                """.formatted(m.group(2).trim()).trim();
    }

    // ============================================================
    // Line-item extraction (noise-tolerant)
    // ============================================================

    private String extractLineItems(String text) {
        if (text == null) return "";

        List<String> lines = Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(l -> l.length() > 5)
                .filter(l -> l.matches(".*\\d+\\.\\d{2}.*"))
                .filter(l -> !l.toLowerCase().matches(".*(total|amount due|balance).*"))
                .limit(12)
                .toList();

        if (lines.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("Line Items:\n");
        for (String line : lines) {
            sb.append("- ").append(line).append("\n");
        }

        return sb.toString().trim();
    }
}
