package com.wells.bill.assistant.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BillAnalysisService {

    // 1) BILL CLASSIFICATION
    @Tool(name = "classifyBill", description = "Classify the bill type (electricity, water, gas, internet, phone).")
    public String classifyBill(@ToolParam(description = "billText") String billText) {
        String text = billText.toLowerCase();
        if (text.contains("kwh") || text.contains("meter") || text.contains("electric")) return "electricity";
        if (text.contains("water")) return "water";
        if (text.contains("gas")) return "gas";
        if (text.contains("internet") || text.contains("broadband")) return "internet";
        if (text.contains("mobile") || text.contains("phone")) return "phone";
        return "unknown";
    }

    // 2) BILL FIELD EXTRACTION
    @Tool(name = "extractBillFields", description = "Extract fields like amount, billingPeriod, vendor, units, dueDate.")
    public Map<String, Object> extract(@ToolParam(description = "billText") String text) {

        Map<String, Object> map = new HashMap<>();

        map.put("totalAmount", regex(text, "total[\\s:]+(\\d+[.,]?\\d*)"));
        map.put("dueDate", regex(text, "due date[\\s:]+([A-Za-z0-9\\/\\-]+)"));
        map.put("billingPeriod", regex(text, "billing period[\\s:]+([A-Za-z0-9\\-\\/\\s]+)"));
        map.put("units", regex(text, "(\\d+)\\s?(kwh|units)"));

        return map;
    }

    private String regex(String text, String pattern) {
        Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    // 3) BILL COMPARISON
    @Tool(name = "compareBills", description = "Compare two bill texts and highlight differences.")
    public String compareBills(
            @ToolParam(description = "bill1") String bill1,
            @ToolParam(description = "bill2") String bill2) {

        return """
                Compare the following two bills and summarize the key differences:
                
                BILL 1:
                %s
                
                BILL 2:
                %s
                """.formatted(bill1, bill2);
    }

    // 4) ANOMALY DETECTION
    @Tool(name = "detectAnomaly", description = "Detect sudden spikes in bill amounts.")
    public String detectAnomaly(@ToolParam(description = "amounts") List<Double> amounts) {
        if (amounts.size() < 2) return "Not enough data.";

        double last = amounts.getLast();
        double prev = amounts.get(amounts.size() - 2);

        if (last > prev * 1.20) {
            return "⚠️ Anomaly detected: bill increased by more than 20%.";
        }

        return "No anomaly detected.";
    }

    // 5) TREND ANALYSIS
    @Tool(name = "trendAnalysis", description = "Analyze bill amount trend.")
    public String trend(@ToolParam(description = "amounts") List<Double> amounts) {
        if (amounts.size() < 3) return "Not enough history.";

        double first = amounts.getFirst();
        double last = amounts.getLast();

        if (last > first) return "Bill trend is increasing.";
        if (last < first) return "Bill trend is decreasing.";
        return "Bill trend is stable.";
    }

    // 6) BILL SUMMARY
    @Tool(name = "summarizeBill", description = "Summarize the bill text.")
    public String summarize(@ToolParam(description = "billText") String text) {
        return """
                    Summarize the following bill:
                    %s
                """.formatted(text);
    }

    // 7) BILL SUGGESTIONS
    @Tool(name = "billSuggestion", description = "Suggest actions based on bill amount and usage.")
    public String suggest(
            @ToolParam(description = "amount") Double amount,
            @ToolParam(description = "units") Integer units,
            @ToolParam(description = "lateFee") Double lateFee) {

        StringBuilder sb = new StringBuilder("Suggestions:\n");
        if (lateFee > 0) sb.append("- Pay before due date to avoid late fees.\n");
        if (units > 250) sb.append("- Usage is high; consider reducing consumption.\n");
        if (amount > 2000) sb.append("- Amount is high; check for anomalies.\n");

        return sb.toString();
    }

    public String executeAnalysisTool(String tool, Map<String, Object> args) {
        return switch (tool) {
            case "classifyBill" -> classifyBill(args.get("billText").toString());
            case "extractBillFields" -> extract(args.get("billText").toString()).toString();
            case "compareBills" -> compareBills(
                    args.get("bill1").toString(),
                    args.get("bill2").toString()
            );
            case "detectAnomaly" -> detectAnomaly((List<Double>) args.get("amounts"));
            case "trendAnalysis" -> trend((List<Double>) args.get("amounts"));
            case "summarizeBill" -> summarize(args.get("billText").toString());
            case "billSuggestion" -> suggest(
                    (Double) args.get("amount"),
                    (Integer) args.get("units"),
                    (Double) args.get("lateFee")
            );
            default -> "Invalid tool: " + tool;
        };
    }
}
