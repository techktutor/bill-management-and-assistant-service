package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.service.BillManagementService;
import com.wells.bill.assistant.service.PaymentService;
import com.wells.bill.assistant.service.RagEngineService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * -------------------------------------------------------------------------
 * BillAssistantTool (UNIFIED BILL TOOL)
 * Combines functionality from:
 * - BillManagementTool
 * - BillQueryTools
 * - BillRagTools
 * - OverdueExplainerTool
 * - BillToolAdaptor
 * -------------------------------------------------------------------------
 */
@Component
@RequiredArgsConstructor
public class BillAssistantTool {

    private static final Logger log = LoggerFactory.getLogger(BillAssistantTool.class);

    private final BillManagementService billService;
    private final PaymentService paymentService;
    private final RagEngineService ragEngineService;

    // -----------------------------
    // A: BILL CRUD & QUERIES
    // -----------------------------

    @Tool(name = "getBillById", description = "Retrieve bill details by bill ID.")
    public BillEntity getBillById(@ToolParam(description = "Bill ID") Long billId) {
        return billService.getBill(billId);
    }

    @Tool(name = "listPendingBills", description = "List all unpaid bills (PENDING or OVERDUE).")
    public List<BillEntity> listPendingBills() {
        return billService.findAllUnpaid();
    }

    @Tool(name = "dueBillsNext7Days", description = "Bills due in next 7 days.")
    public List<BillEntity> dueBillsNext7Days() {
        return billService.findByDueDateRange(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Tool(name = "findUpcomingUnpaidBills", description = "Unpaid bills due in next 7 days.")
    public List<BillEntity> findUpcomingUnpaidBills() {
        return billService.findUnpaidByDueDateRange(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Tool(name = "showUnpaidGroupedByVendor", description = "Group unpaid bills by vendor.")
    public Map<String, List<BillEntity>> showUnpaidGroupedByVendor() {
        return billService.findAllUnpaid().stream()
                .collect(Collectors.groupingBy(b -> b.getVendor() == null ? "unknown" : b.getVendor()));
    }

    @Tool(name = "markBillPaid", description = "Mark bill as PAID.")
    public String markBillPaid(Long billId) {
        billService.markAsPaid(billId);
        return "Bill marked as PAID: " + billId;
    }

    // -----------------------------
    // B: RAG & SEMANTIC REASONING
    // -----------------------------

    @Tool(name = "ragRetrieveBillContext", description = "Retrieve stitched RAG context for a bill.")
    public String ragRetrieveBillContext(String billId) {
        List<Document> docs = ragEngineService.retrieveByBillIdHybrid(billId, "bill context", 12);
        if (docs.isEmpty()) return "No bill context found.";
        return ragEngineService.stitchContext(docs, 8000);
    }

    @Tool(name = "answerBillQuestion", description = "Answer questions using RAG on bill context.")
    public String answerBillQuestion(
            @ToolParam(description = "Bill ID") String billId,
            @ToolParam(description = "User question") String question) {
        return ragEngineService.answerQuestionForBill(billId, question);
    }

    @Tool(name = "explainWhyOverdue", description = "Explain overdue reason using RAG.")
    public String explainWhyOverdue(String billId) {
        return ragEngineService.answerQuestionForBill(billId, "Explain why this bill is overdue.");
    }

    // -----------------------------
    // C: BILL TEXT EXTRACTION / ANALYTICS (MERGED FROM BillToolAdaptor)
    // -----------------------------

    /**
     * Normalizer
     */
    private String normalize(String input) {
        return input == null ? "" : input.replace("\n", " ")
                .replace("\r", " ").replaceAll("\\s+", " ").trim();
    }

    // Regex for extraction
    private static final Pattern AMOUNT = Pattern.compile(
            "(?:total\\s*amount|amount\\s*due|total|due)[^0-9]*" +
                    "([₹$]?\\s*[0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DUE_DATE = Pattern.compile(
            "(?:due\\s*date|payment\\s*due|due\\s*by)[^A-Za-z0-9]*" +
                    "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}|\\d{1,2}\\s*[A-Za-z]{3,9}\\s*\\d{2,4}|[A-Za-z]{3,9}\\s*\\d{1,2},?\\s*\\d{2,4})",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, Pattern> CATEGORY_PATTERNS = Map.of(
            "electricity", Pattern.compile("(kwh|kilowatt|meter|electric|consumption)", Pattern.CASE_INSENSITIVE),
            "water", Pattern.compile("(water|gallons|m3|cubic)", Pattern.CASE_INSENSITIVE),
            "gas", Pattern.compile("(gas|therm|mmbtu|cubic feet)", Pattern.CASE_INSENSITIVE),
            "internet", Pattern.compile("(internet|broadband|bandwidth|isp)", Pattern.CASE_INSENSITIVE),
            "phone", Pattern.compile("(mobile|phone|telecom|cellular)", Pattern.CASE_INSENSITIVE)
    );

    @Tool(name = "classifyBill", description = "Classify bill category from text.")
    public String classifyBill(String billText) {
        billText = normalize(billText);
        if (billText.isBlank()) return "unknown";

        String finalBillText = billText;
        return CATEGORY_PATTERNS.entrySet().stream()
                .filter(e -> e.getValue().matcher(finalBillText).find())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("unknown");
    }

    @Tool(name = "extractBillFields", description = "Extract fields like amount, due date, vendor, usage.")
    public Map<String, Object> extractBillFields(String text) {
        text = normalize(text);
        if (text.isBlank()) return Map.of();

        Map<String, Object> result = new HashMap<>();

        // Amount
        Matcher mAmount = AMOUNT.matcher(text);
        result.put("totalAmount", mAmount.find()
                ? mAmount.group(1).replace(",", "").replace("$", "").trim()
                : "unknown");

        // Due date
        Matcher mDue = DUE_DATE.matcher(text);
        result.put("dueDate", mDue.find() ? mDue.group(1).trim() : "unknown");

        // Vendor
        result.put("vendorName", extractRegex(text, "(?:from|biller|vendor|company)[:\\s]+([A-Za-z\\s&]+)"));

        // Billing period
        result.put("billingPeriod", extractRegex(text, "(?:billing\\s*period|period)[\\s:]+([A-Za-z0-9\\-/\\s,]+)"));

        // Units
        result.put("units", extractRegex(text, "(\\d+(?:\\.\\d+)?)\\s*(?:kwh|units|gallons|therm)"));

        return result;
    }

    private String extractRegex(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1).trim() : "unknown";
    }

    @Tool(name = "summarizeBill", description = "Summarize bill from text.")
    public String summarizeBill(String text) {
        text = normalize(text);
        if (text.isBlank()) return "Cannot summarize empty bill.";

        Map<String, Object> f = extractBillFields(text);

        return """
                Bill Summary:
                - Type: %s
                - Amount: %s
                - Due Date: %s
                - Billing Period: %s
                - Usage: %s units
                """.formatted(
                classifyBill(text),
                f.get("totalAmount"),
                f.get("dueDate"),
                f.get("billingPeriod"),
                f.get("units")
        );
    }

    @Tool(name = "compareBills", description = "Compare two bill texts.")
    public String compareBills(String bill1, String bill2) {
        if (bill1 == null || bill1.isBlank() || bill2 == null || bill2.isBlank()) {
            return "Both bills must contain text.";
        }

        Map<String, Object> f1 = extractBillFields(bill1);
        Map<String, Object> f2 = extractBillFields(bill2);

        return """
                Bill Comparison:
                Amount 1: %s
                Amount 2: %s
                Due Date 1: %s
                Due Date 2: %s
                """.formatted(
                f1.get("totalAmount"), f2.get("totalAmount"),
                f1.get("dueDate"), f2.get("dueDate")
        );
    }

    @Tool(name = "detectAnomaly", description = "Detect >20% bill increase.")
    public String detectAnomaly(List<Double> amounts) {
        if (amounts == null || amounts.size() < 2) return "Not enough history.";

        double last = amounts.getLast();
        double prev = amounts.get(amounts.size() - 2);

        if (prev <= 0) return "Invalid previous amount.";

        double change = ((last - prev) / prev) * 100;

        return change > 20
                ? String.format("Anomaly detected: increased %.1f%%", change)
                : String.format("No anomaly: %.1f%% change", change);
    }

    @Tool(name = "trendAnalysis", description = "Analyze bill trend.")
    public String trendAnalysis(List<Double> amounts) {
        if (amounts == null || amounts.size() < 3) return "Not enough history.";

        double first = amounts.getFirst();
        double last = amounts.getLast();

        if (last > first * 1.15) return "Trend: increasing";
        if (last < first * 0.85) return "Trend: decreasing";
        return "Trend: stable";
    }

    @Tool(name = "billSuggestion", description = "Actionable suggestions based on bill metrics.")
    public String billSuggestion(Double amount, Integer units, Double lateFee) {
        if (amount == null || lateFee == null) return "Invalid metrics.";

        StringBuilder sb = new StringBuilder("Suggestions:\n");

        if (lateFee > 0) sb.append("- Avoid late fee of $").append(lateFee).append(".\n");
        if (units != null && units > 250) sb.append("- High usage: consider reducing consumption.\n");
        if (amount > 2000) sb.append("- Large bill amount: review charges.\n");

        return sb.toString();
    }

    // -----------------------------
    // D: PAYMENT HISTORY
    // -----------------------------

    @Tool(name = "howMuchPaidLastMonth", description = "Total paid by customer last month.")
    public String howMuchPaidLastMonth(String customerIdStr) {
        try {
            UUID customerId = UUID.fromString(customerIdStr);
            YearMonth lastMonth = YearMonth.now().minusMonths(1);

            long sumCents = paymentService.findPaymentsByCustomer(customerId).stream()
                    .filter(p -> YearMonth.from(p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()).equals(lastMonth))
                    .mapToLong(PaymentEntity::getAmount)
                    .sum();

            return String.format("Total paid in %s: $%.2f", lastMonth, sumCents / 100.0);
        } catch (Exception e) {
            return "Invalid customer ID.";
        }
    }
}
