package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.BillAnomalyReport;
import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.BillExplanation;
import com.wells.bill.assistant.model.BillStatus;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.store.ContextStore;
import com.wells.bill.assistant.util.ConversationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillAssistantTool {

    private final BillService billService;
    private final ContextStore contextStore;

    /* =====================================================
     * 1️⃣ READ-ONLY BILL QUERIES (SAFE FOR AI)
     * ===================================================== */

    @Tool(
            name = "getBillDetails",
            description = "Get complete details of a specific bill using its billId."
    )
    public BillDetail getBillDetails(
            @ToolParam(description = "Bill Provider Name") String billName
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        log.info("BillAssistantTool: Fetching bill details for billName={}", billName);

        return getDetails(userId, billName);
    }

    @Tool(
            name = "listAllBills",
            description = "List all bills for a given user. Use pagination at the UI layer if needed."
    )
    public List<BillDetail> listAllBills() {

        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: listAllBills for userId={} and conversationId={}", userId, conversationId);

        return billService.getBills(userId, Pageable.unpaged()).getContent();
    }

    @Tool(
            name = "listUnpaidBills",
            description = """
                    List all unpaid bills for a user.
                    Includes bills in UPLOADED, INGESTED, VERIFIED, or OVERDUE states.
                    """
    )
    public List<BillDetail> listUnpaidBills() {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: listUnpaidBills for userId={} and conversationId={}", userId, conversationId);

        return billService.getUnpaidBills(userId);
    }

    @Tool(
            name = "listBillsDueSoon",
            description = """
                    Find unpaid bills that are due soon.
                    Useful for reminders and alerts.
                    """
    )
    public List<BillDetail> listBillsDueSoon() {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: listBillsDueSoon for userId={} and conversationId={}", userId, conversationId);

        LocalDate endDate = LocalDate.now().plusDays(15);

        return billService.getUnpaidBills(userId)
                .stream()
                .filter(b ->
                        b.dueDate() != null &&
                                !b.dueDate().isAfter(endDate)
                )
                .toList();
    }

    @Tool(
            name = "listBillsDueAfter",
            description = """
                    Find unpaid bills that are due within the next N days.
                    Useful for reminders and alerts.
                    """
    )
    public List<BillDetail> listBillsDueAfter(
            @ToolParam(description = "Number of days from today") int days
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: listBillsDueAfter for userId={}, conversationId={}", userId, conversationId);

        LocalDate endDate = LocalDate.now().plusDays(days);

        return billService.getUnpaidBills(userId)
                .stream()
                .filter(b ->
                        b.dueDate() != null &&
                                !b.dueDate().isAfter(endDate)
                )
                .toList();
    }

    @Tool(
            name = "groupUnpaidBillsByProvider",
            description = """
                    Group all unpaid bills by provider name (vendor).
                    Helps summarize spending by service provider.
                    """
    )
    public Map<String, List<BillDetail>> groupUnpaidBillsByProvider() {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: groupUnpaidBillsByProvider for userId={}, conversationId={}", userId, conversationId);

        return billService.getUnpaidBills(userId)
                .stream()
                .collect(Collectors.groupingBy(
                        b -> b.providerName() != null
                                ? b.providerName()
                                : "UNKNOWN_PROVIDER"
                ));
    }

    /* =====================================================
     * 2️⃣ STATE-CHANGING ACTIONS (EXPLICIT & GUARDED)
     * ===================================================== */

    @Tool(
            name = "markBillAsVerified",
            description = """
                    Mark a bill as VERIFIED.
                    This action is irreversible and changes bill state.
                    Use only after bill data has been reviewed.
                    """
    )
    public BillDetail markBillAsVerified(@ToolParam(description = "Bill provider name") String providerName) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: markBillAsVerified for providerName={}, userId={}, conversationId={}", providerName, userId, conversationId);

        return billService.markVerified(getDetails(userId, providerName).id(), userId);
    }

    @Tool(
            name = "explainBill",
            description = """
                    Explain a bill in plain English for the user.
                    This tool is read-only and does not modify bill state.
                    Useful for answering questions like:
                    - What is this bill?
                    - Why do I need to pay it?
                    - Is it overdue?
                    """
    )
    public BillExplanation explainBill(@ToolParam(description = "Bill provider name") String providerName) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: explaining bill for providerName={}, userId={}, conversationId={}", providerName, userId, conversationId);

        BillDetail bill = getDetails(userId, providerName);

        boolean overdue = bill.status() == BillStatus.OVERDUE;
        boolean payable = bill.status() != BillStatus.PAID
                && bill.status() != BillStatus.CANCELLED;

        String summary = """
                This is a %s bill from %s.
                The bill amount is %s %s and covers usage from %s to %s.
                The payment is due on %s.
                Current status of the bill is %s.
                %s
                """.formatted(
                bill.billCategory(),
                bill.providerName(),
                bill.amountDue(),
                bill.amountDue().currency().getSymbol(),
                bill.billingPeriod().start(),
                bill.billingPeriod().end(),
                bill.dueDate(),
                bill.status(),
                overdue
                        ? "This bill is overdue and should be paid immediately."
                        : payable
                        ? "This bill is payable."
                        : "This bill has already been settled."
        );

        return new BillExplanation(
                bill.id(),
                bill.providerName(),
                bill.consumerName(),
                bill.serviceNumber(),
                bill.amountDue().amount(),
                bill.amountDue().currency().getSymbol(),
                bill.billingPeriod().start(),
                bill.billingPeriod().end(),
                bill.dueDate(),
                bill.status().name(),
                overdue,
                payable,
                summary.strip()
        );
    }

    @Tool(
            name = "explainWhyBillIsHigh",
            description = """
                    Explain possible reasons why a bill amount is higher than usual.
                    This is a heuristic explanation and does not modify any data.
                    """
    )
    public String explainWhyBillIsHigh(
            @ToolParam(description = "Bill provider name") String providerName
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: explaining why bill is high for providerName={}, userId={}, conversationId={}", providerName, userId, conversationId);

        BillDetail bill = getDetails(userId, providerName);

        StringBuilder explanation = new StringBuilder(
                "Here are possible reasons this bill might be higher than expected:\n"
        );

        if (bill.billingPeriod().start() != null && bill.billingPeriod().end() != null) {
            long days = bill.billingPeriod().end().toEpochDay()
                    - bill.billingPeriod().start().toEpochDay();
            if (days > 31) {
                explanation.append("- The billing period is longer than usual.\n");
            }
        }

        explanation.append("""
                - Increased usage during the billing period
                - Seasonal factors (e.g., weather-related usage)
                - Tariff or rate changes by the provider
                - Late fees or adjustments from previous cycles
                """);

        return explanation.toString().strip();
    }

    @Tool(
            name = "suggestPaymentPriority",
            description = """
                    Suggest how urgently a bill should be paid based on due date and status.
                    Returns HIGH, MEDIUM, or LOW priority.
                    """
    )
    public String suggestPaymentPriority(
            @ToolParam(description = "Bill provider name") String providerName
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: suggesting payment priority for providerName={}, userId={}, conversationId={}", providerName, userId, conversationId);

        BillDetail bill = getDetails(userId, providerName);

        if (bill.status() == BillStatus.OVERDUE) {
            return "HIGH priority – this bill is overdue.";
        }

        if (bill.dueDate() != null && bill.dueDate().isBefore(LocalDate.now().plusDays(3))) {
            return "HIGH priority – due very soon.";
        }

        if (bill.dueDate() != null && bill.dueDate().isBefore(LocalDate.now().plusDays(7))) {
            return "MEDIUM priority – upcoming due date.";
        }

        return "LOW priority – no immediate action required.";
    }

    @Tool(
            name = "generatePaymentReminderMessage",
            description = """
                    Generate a friendly reminder message for paying a bill.
                    Suitable for notifications, email, or WhatsApp.
                    """
    )
    public String generatePaymentReminderMessage(
            @ToolParam(description = "Bill provider name") String providerName
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: generating payment reminder for providerName={}, userId={}, conversationId={}", providerName, userId, conversationId);

        BillDetail bill = getDetails(userId, providerName);

        return """
                Reminder: Your %s bill from %s amounting to %s %s
                is due on %s.
                Please make the payment to avoid late fees.
                """
                .formatted(
                        bill.billCategory(),
                        bill.providerName(),
                        bill.amountDue(),
                        bill.amountDue().currency().getSymbol(),
                        bill.dueDate()
                )
                .strip();
    }

    @Tool(
            name = "explainBillAsBulletPoints",
            description = """
                    Explain a bill as simple bullet points for easy understanding.
                    Read-only helper for UI or conversational display.
                    """
    )
    public List<String> explainBillAsBulletPoints(
            @ToolParam(description = "Bill provider name") String providerName
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: explaining bill as bullet points for providerName={}, userId={}, conversationId={}", providerName, userId, conversationId);

        BillDetail bill = getDetails(userId, providerName);

        return List.of(
                "Provider: " + bill.providerName(),
                "Category: " + bill.billCategory(),
                "Service Number: " + bill.serviceNumber(),
                "Amount Due: " + bill.amountDue() + " " + bill.amountDue().currency().getSymbol(),
                "Billing Period: " + bill.billingPeriod().start() + " to " + bill.billingPeriod().end(),
                "Due Date: " + bill.dueDate(),
                "Current Status: " + bill.status()
        );
    }

    @Tool(
            name = "explainBillInRegionalLanguage",
            description = """
                    Explain a bill in a regional language such as Hindi or English.
                    Language code examples: en, hi.
                    """
    )
    public String explainBillInRegionalLanguage(
            @ToolParam(description = "Bill provider name") String providerName,
            @ToolParam(description = "Language code (en, hi)") String language
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();
        log.info("BillAssistantTool: explaining bill in language={} for providerName={}, userId={}, conversationId={}", language, providerName, userId, conversationId);

        BillDetail bill = getDetails(userId, providerName);

        return switch (language.toLowerCase()) {
            case "hi" -> """
                    यह %s का बिल है।
                    सेवा प्रदाता: %s
                    देय राशि: %s %s
                    अंतिम भुगतान तिथि: %s
                    वर्तमान स्थिति: %s
                    """
                    .formatted(
                            bill.billCategory(),
                            bill.providerName(),
                            bill.amountDue(),
                            bill.amountDue().currency().getSymbol(),
                            bill.dueDate(),
                            bill.status()
                    );

            default -> """
                    This is a %s bill from %s.
                    Amount due: %s %s.
                    Due date: %s.
                    Current status: %s.
                    """
                    .formatted(
                            bill.billCategory(),
                            bill.providerName(),
                            bill.amountDue(),
                            bill.amountDue().currency().getSymbol(),
                            bill.dueDate(),
                            bill.status()
                    );
        };
    }

    @Tool(
            name = "forecastMonthlySpend",
            description = """
                    Forecast expected monthly spend based on current unpaid bills.
                    This is an estimate and does not affect any data.
                    """
    )
    public String forecastMonthlySpend() {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: forecasting monthly spend for userId={}, conversationId={}", userId, conversationId);

        List<BillDetail> unpaid = billService.getUnpaidBills(userId);

        if (unpaid.isEmpty()) {
            return "No unpaid bills found. Unable to forecast spending.";
        }

        double total = unpaid.stream()
                .mapToDouble(b -> b.amountDue().amount().doubleValue())
                .sum();

        double average = total / unpaid.size();

        return """
                Based on your current unpaid bills:
                - Total outstanding amount: %.2f
                - Estimated average monthly spend: %.2f
                """
                .formatted(total, average);
    }

    @Tool(
            name = "detectBillAnomaly",
            description = """
                    Detect whether a bill appears unusual compared to a user's other bills.
                    Uses explainable heuristics such as amount deviation, provider novelty,
                    due-date urgency, and category frequency.
                    Read-only AI helper tool.
                    """
    )
    public BillAnomalyReport detectBillAnomaly(
            @ToolParam(description = "Bill provider name") String providerName
    ) {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound to tool execution");
        }

        UUID conversationId = ConversationContextHolder.getConversationId();

        log.info("BillAssistantTool: detecting anomaly for providerName={}, userId={}, conversationId={}", providerName, userId, conversationId);

        BillDetail current = getDetails(userId, providerName);
        List<BillDetail> unpaid = billService.getUnpaidBills(userId);

        List<String> signals = new ArrayList<>();
        int score = 0;

        /* -----------------------------------
         * 1️⃣ Amount anomaly
         * ----------------------------------- */
        double averageAmount = unpaid.stream()
                .filter(b -> !b.id().equals(current.id()))
                .mapToDouble(b -> b.amountDue().amount().doubleValue())
                .average()
                .orElse(current.amountDue().amount().doubleValue());

        if (current.amountDue().amount().doubleValue() > averageAmount * 1.5) {
            signals.add("Bill amount is significantly higher than your usual bills.");
            score += 30;
        }

        /* -----------------------------------
         * 2️⃣ Category anomaly
         * ----------------------------------- */
        long sameCategoryCount = unpaid.stream()
                .filter(b -> b.billCategory() == current.billCategory())
                .count();

        if (sameCategoryCount <= 1) {
            signals.add("This bill category is uncommon for you.");
            score += 15;
        }

        /* -----------------------------------
         * 3️⃣ Provider anomaly
         * ----------------------------------- */
        long sameProviderCount = unpaid.stream()
                .filter(b -> current.providerName() != null
                        && current.providerName().equalsIgnoreCase(b.providerName()))
                .count();

        if (sameProviderCount <= 1) {
            signals.add("This provider is new or rarely used.");
            score += 15;
        }

        /* -----------------------------------
         * 4️⃣ Due-date urgency anomaly
         * ----------------------------------- */
        if (current.dueDate() != null &&
                current.dueDate().isBefore(LocalDate.now().plusDays(2))) {
            signals.add("Very short time between bill creation and due date.");
            score += 20;
        }

        /* -----------------------------------
         * 5️⃣ Status anomaly
         * ----------------------------------- */
        if (current.status() == BillStatus.OVERDUE) {
            signals.add("Bill became overdue unusually fast.");
            score += 20;
        }

        boolean anomalous = score >= 40;

        String summary = anomalous
                ? "This bill appears unusual and may require your attention."
                : "This bill looks consistent with your usual billing patterns.";

        return new BillAnomalyReport(
                current.id(),
                anomalous,
                Math.min(score, 100),
                current.amountDue().amount(),
                BigDecimal.valueOf(averageAmount),
                signals,
                summary
        );
    }

    private BillDetail getDetails(UUID userId, String providerName) {
        List<BillDetail> bills = billService.findBillsByProviderName(userId, providerName);
        if (bills.size() > 1) {
            log.warn("Multiple bills found for provider name={}", providerName);
        } else if (bills.isEmpty()) {
            log.error("No bills found for provider name={}", providerName);
            throw new IllegalArgumentException("No bills found for provider name: " + providerName);
        }
        return bills.getFirst();
    }
}
