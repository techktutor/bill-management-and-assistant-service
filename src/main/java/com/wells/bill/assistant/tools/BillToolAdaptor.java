package com.wells.bill.assistant.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BillToolAdaptor {

    private static final Logger log = LoggerFactory.getLogger(BillToolAdaptor.class);

    // Normalization helper
    private String normalize(String text) {
        return text == null ? "" : text.replace("\n", " ").replace("\r", " ").replaceAll("\\s+", " ").trim();
    }

    // Improved regex patterns
    private static final Pattern AMOUNT = Pattern.compile("(?:total\\s*amount|amount\\s*due|total|due)[^0-9]*" + "([₹$]?\\s*[0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DUE_DATE = Pattern.compile("(?:due\\s*date|payment\\s*due|due\\s*by)[^A-Za-z0-9]*" + "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|" + "\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}|" + "\\d{1,2}\\s*[A-Za-z]{3,9}\\s*\\d{2,4}|" + "[A-Za-z]{3,9}\\s*\\d{1,2},?\\s*\\d{2,4})", Pattern.CASE_INSENSITIVE);

    private static final Map<String, Pattern> PATTERNS = Map.ofEntries(Map.entry("electricity", Pattern.compile("(kwh|kilowatt|meter|electric|consumption)", Pattern.CASE_INSENSITIVE)), Map.entry("water", Pattern.compile("(water|gallons|m3|cubic)", Pattern.CASE_INSENSITIVE)), Map.entry("gas", Pattern.compile("(gas|therm|mmbtu|cubic feet)", Pattern.CASE_INSENSITIVE)), Map.entry("internet", Pattern.compile("(internet|broadband|bandwidth|isp)", Pattern.CASE_INSENSITIVE)), Map.entry("phone", Pattern.compile("(mobile|phone|telecom|cellular)", Pattern.CASE_INSENSITIVE)));

    @Tool(name = "classifyBill", description = "Classify the bill type based on content. Returns electricity, water, gas, internet, phone, or unknown.")
    public String classifyBill(@ToolParam(description = "Full bill text content") String billText) {
        log.info("Classifying bill text of length: {}", billText == null ? 0 : billText.length());
        billText = normalize(billText);
        if (billText.isBlank()) return "unknown";

        for (var entry : PATTERNS.entrySet()) {
            if (entry.getValue().matcher(billText).find()) {
                return entry.getKey();
            }
        }
        return "unknown";
    }

    @Tool(name = "extractBillFields", description = "Extract key fields: totalAmount, dueDate, billingPeriod, units, vendorName")
    public Map<String, Object> extract(@ToolParam(description = "Full bill text content") String text) {
        log.info("Extracting bill fields from text of length: {}", text == null ? 0 : text.length());
        text = normalize(text);
        if (text.isBlank()) return Map.of();

        Map<String, Object> map = new HashMap<>();

        map.put("totalAmount", extractAmount(text));
        map.put("dueDate", extractField(text, DUE_DATE));
        map.put("billingPeriod", extractField(text, "(?:billing\\s*period|period)[\\s:]+([A-Za-z0-9\\-\\/\\s,]+)"));
        map.put("units", extractField(text, "(\\d+(?:\\.\\d+)?)\\s*(?:kwh|units|gallons|therm)"));
        map.put("vendorName", extractField(text, "(?:from|biller|vendor|company)[\\s:]+([A-Za-z\\s&]+)"));

        return map;
    }

    private String extractAmount(String text) {
        Matcher matcher = AMOUNT.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replace(",", "").replace("₹", "").replace("$", "").trim();
        }
        return "unknown";
    }

    private String extractField(String text, Pattern pattern) {
        try {
            Matcher m = pattern.matcher(text);
            if (m.find()) return m.group(1).trim();
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String extractField(String text, String regex) {
        return extractField(text, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }

    @Tool(name = "compareBills", description = "Compare two bills and highlight differences in amount and due date.")
    public String compareBills(String bill1, String bill2) {
        log.info("Comparing two bills of lengths: {}, {}", bill1 == null ? 0 : bill1.length(), bill2 == null ? 0 : bill2.length());
        if (bill1 == null || bill1.isBlank() || bill2 == null || bill2.isBlank()) {
            return "Error: Both bills must contain text.";
        }

        Map<String, Object> f1 = extract(bill1);
        Map<String, Object> f2 = extract(bill2);

        return "Bill Comparison:\n" + "Amount 1: " + f1.get("totalAmount") + "\n" + "Amount 2: " + f2.get("totalAmount") + "\n" + "Due Date 1: " + f1.get("dueDate") + "\n" + "Due Date 2: " + f2.get("dueDate") + "\n";
    }

    @Tool(name = "detectAnomaly", description = "Detect >20% bill increase using last two data points.")
    public String detectAnomaly(List<Double> amounts) {
        log.info("Detecting anomaly in amounts history of size: {}", amounts == null ? 0 : amounts.size());
        if (amounts == null || amounts.size() < 2) {
            return "Not enough history to detect anomalies.";
        }

        double last = amounts.getLast();
        double prev = amounts.get(amounts.size() - 2);

        if (prev <= 0) return "Invalid previous amount.";

        double change = ((last - prev) / prev) * 100;

        if (change > 20) return String.format("Anomaly detected: increased %.1f%%", change);

        return String.format("No anomaly: %.1f%% change", change);
    }

    @Tool(name = "trendAnalysis", description = "Analyze trend: increasing, decreasing, or stable.")
    public String trend(List<Double> amounts) {
        log.info("Analyzing trend in amounts history of size: {}", amounts == null ? 0 : amounts.size());
        if (amounts == null || amounts.size() < 3) return "Not enough history for trend analysis.";

        double first = amounts.getFirst();
        double last = amounts.getLast();

        if (last > first * 1.15) return "Trend: increasing";
        if (last < first * 0.85) return "Trend: decreasing";
        return "Trend: stable";
    }

    @Tool(name = "summarizeBill", description = "Summarize bill using extracted structured fields.")
    public String summarize(String text) {
        log.info("Summarizing bill text of length: {}", text == null ? 0 : text.length());
        text = normalize(text);

        if (text.isBlank()) return "Cannot summarize empty bill.";

        Map<String, Object> f = extract(text);

        return String.format("Bill Summary:\n- Type: %s\n- Amount: %s\n- Due Date: %s\n- Billing Period: %s\n- Usage: %s units", classifyBill(text), f.get("totalAmount"), f.get("dueDate"), f.get("billingPeriod"), f.get("units"));
    }

    @Tool(name = "billSuggestion", description = "Provide actionable suggestions based on metrics.")
    public String suggest(Double amount, Integer units, Double lateFee) {
        log.info("Generating suggestions for amount: {}, units: {}, lateFee: {}", amount, units, lateFee);
        if (amount == null || amount < 0 || lateFee == null || lateFee < 0) {
            return "Invalid bill metrics provided.";
        }

        StringBuilder sb = new StringBuilder("Suggestions:\n");

        if (lateFee > 0)
            sb.append("- Avoid late fee of $").append(String.format("%.2f", lateFee)).append(" by paying early.\n");
        if (units != null && units > 250)
            sb.append("- High usage (").append(units).append("): consider reducing consumption.\n");
        if (amount > 2000)
            sb.append("- High bill ($").append(String.format("%.2f", amount)).append(") consider checking for errors.\n");

        return sb.toString();
    }
}
