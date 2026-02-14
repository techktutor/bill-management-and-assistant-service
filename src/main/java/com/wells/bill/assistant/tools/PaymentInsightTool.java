package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.CategorySpendSummary;
import com.wells.bill.assistant.model.MonthlyPaymentSummary;
import com.wells.bill.assistant.model.PaymentResponse;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.PaymentService;
import com.wells.bill.assistant.util.ConversationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInsightTool {

    private final PaymentService paymentService;
    private final BillService billService;

    /* =====================================================
     * 1️⃣ PAYMENT ANOMALY EXPLANATION (CHATBOT FRIENDLY)
     * ===================================================== */

    @Tool(
            name = "explainPaymentAnomaly",
            description = """
                    Check whether the most recent payment to a bill provider
                    looks unusual or risky, and explain it in plain English.
                    Useful for fraud or warning messages.
                    """
    )
    public String explainPaymentAnomaly(
            @ToolParam(description = "Bill provider name (example: Electricity, Rent)")
            String providerName
    ) {
        UUID userId = requireUser();

        log.info("Checking payment anomaly for userId={}, provider={}",
                userId, providerName);

        PaymentResponse current = getMostRecentPayment(providerName, userId);
        List<PaymentResponse> allPayments = paymentService.getPaymentsForUser(userId);

        List<String> signals = new ArrayList<>();
        int score = 0;

        // ✅ BigDecimal-safe average calculation
        List<BigDecimal> pastAmounts = allPayments.stream()
                .filter(p -> !p.getPaymentId().equals(current.getPaymentId()))
                .map(PaymentResponse::getAmount)
                .toList();

        BigDecimal avgAmount = pastAmounts.isEmpty()
                ? current.getAmount()
                : pastAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(pastAmounts.size()), 2, RoundingMode.HALF_UP);

        // Amount anomaly
        if (current.getAmount()
                .compareTo(avgAmount.multiply(BigDecimal.valueOf(1.5))) > 0) {
            signals.add("The payment amount is much higher than your usual spending.");
            score += 30;
        }

        // Failure signal
        if (current.getFailureReason() != null) {
            signals.add("This payment has a failure reason recorded.");
            score += 25;
        }

        // Scheduled far in future
        if (current.getScheduledDate() != null &&
                current.getScheduledDate().isAfter(LocalDate.now().plusMonths(3))) {
            signals.add("This payment is scheduled unusually far in the future.");
            score += 15;
        }

        boolean anomalous = score >= 40;

        // ✅ Friendly chatbot response
        if (!anomalous) {
            return """
                    ✅ Your most recent payment to %s looks normal.
                    
                    Amount: %s %s
                    No unusual activity detected.
                    """
                    .formatted(
                            providerName,
                            current.getAmount(),
                            current.getCurrency()
                    );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ Your most recent payment to ")
                .append(providerName)
                .append(" looks unusual.\n\n");

        sb.append("Amount: ")
                .append(current.getAmount())
                .append(" ")
                .append(current.getCurrency())
                .append("\n\n");

        sb.append("Possible reasons:\n");

        for (String s : signals) {
            sb.append("• ").append(s).append("\n");
        }

        sb.append("\nIf this wasn’t expected, you may want to review or contact support.");

        log.info("Payment anomaly detected for user {}: score {}, reasons: {}",
                userId, score, String.join("; ", signals)
        );
        return sb.toString();
    }

    /* =====================================================
     * 2️⃣ EXPLAIN MONTHLY PAYMENT SUMMARY (CHATBOT FRIENDLY)
     * ===================================================== */

    @Tool(
            name = "explainMonthlyPaymentSummary",
            description = """
                    Explain the user's monthly payment activity in plain English.
                    Includes totals, successful payments, failures, and scheduled payments.
                    """
    )
    public String explainMonthlyPaymentSummary(
            @ToolParam(description = "Month in YYYY-MM format (example: 2026-02)")
            String month
    ) {
        UUID userId = requireUser();
        MonthlyPaymentSummary summary = buildMonthlyPaymentSummary(userId, month);

        log.info("Monthly payment summary for user {}:", userId);

        if (summary.totalPayments() == 0) {
            return """
                    📊 Monthly Payment Summary
                    
                    You didn’t make any payments in %s.
                    """
                    .formatted(summary.month());
        }

        return """
                📊 Your Payment Summary for %s
                
                You made **%d payments** totaling **%s**.
                
                ✅ Successful: %s
                ❌ Failed: %s
                📅 Scheduled: %s
                
                Want a category-wise breakdown too?
                """
                .formatted(
                        summary.month(),
                        summary.totalPayments(),
                        summary.totalAmount(),
                        summary.successfulAmount(),
                        summary.failedAmount(),
                        summary.scheduledAmount()
                );
    }

    /* =====================================================
     * 3️⃣ EXPLAIN CATEGORY-WISE SPENDING (CHATBOT FRIENDLY)
     * ===================================================== */

    @Tool(
            name = "explainCategoryWiseSpendSummary",
            description = """
                    Explain category-wise spending (Rent, Utilities, etc.)
                    for a selected month in plain English.
                    """
    )
    public String explainCategoryWiseSpendSummary(
            @ToolParam(description = "Month in YYYY-MM format (example: 2026-02)")
            String month
    ) {
        UUID userId = requireUser();
        CategorySpendSummary summary = buildCategorySpendSummary(userId, month);

        log.info("Category-wise spend summary for user {}:", userId);

        if (summary.categoryTotals().isEmpty()) {
            return "No payments were made in " + summary.month() + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📌 Category-wise Spending for ").append(summary.month()).append(":\n\n");

        summary.categoryTotals().forEach((category, amount) ->
                sb.append("• ").append(category).append(": ").append(amount).append("\n")
        );

        sb.append("\nLet me know if you want details for any category.");

        return sb.toString();
    }

    /* =====================================================
     * INTERNAL HELPERS (NOT TOOLS)
     * ===================================================== */

    private UUID requireUser() {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound.");
        }
        return userId;
    }

    private PaymentResponse getMostRecentPayment(String providerName, UUID userId) {

        List<PaymentResponse> payments = paymentService.findByUserIdAndBillProviderName(userId, providerName);

        if (payments.isEmpty()) {
            throw new InvalidUserInputException(
                    "No payments found for provider: " + providerName
            );
        }

        return payments.stream()
                .filter(p -> p.getCreatedAt() != null)
                .max(Comparator.comparing(PaymentResponse::getCreatedAt))
                .orElse(payments.getFirst());
    }

    private MonthlyPaymentSummary buildMonthlyPaymentSummary(UUID userId, String month) {

        YearMonth yearMonth = YearMonth.parse(month);

        List<PaymentResponse> payments =
                paymentService.getPaymentsForUser(userId).stream()
                        .filter(p -> p.getCreatedAt() != null)
                        .filter(p ->
                                YearMonth.from(
                                        p.getCreatedAt().atZone(ZoneId.systemDefault())
                                ).equals(yearMonth)
                        )
                        .toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal success = BigDecimal.ZERO;
        BigDecimal failed = BigDecimal.ZERO;
        BigDecimal scheduled = BigDecimal.ZERO;

        for (PaymentResponse p : payments) {
            total = total.add(p.getAmount());

            switch (p.getStatus()) {
                case SUCCESS -> success = success.add(p.getAmount());
                case FAILED -> failed = failed.add(p.getAmount());
                case APPROVAL_PENDING -> {
                    if (p.getScheduledDate() != null) {
                        scheduled = scheduled.add(p.getAmount());
                    }
                }
            }
        }

        return new MonthlyPaymentSummary(
                userId,
                yearMonth,
                payments.size(),
                total,
                success,
                failed,
                scheduled,
                List.of(),
                ""
        );
    }

    private CategorySpendSummary buildCategorySpendSummary(UUID userId, String month) {

        YearMonth yearMonth = YearMonth.parse(month);

        List<PaymentResponse> payments =
                paymentService.getPaymentsForUser(userId).stream()
                        .filter(p -> p.getCreatedAt() != null)
                        .filter(p -> YearMonth.from(
                                p.getCreatedAt().atZone(ZoneId.systemDefault())
                        ).equals(yearMonth))
                        .toList();

        Set<UUID> billIds = payments.stream()
                .map(PaymentResponse::getBillId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, BillDetail> billMap = billService.getBillsByIds(userId, billIds);

        Map<String, BigDecimal> totals = new HashMap<>();

        for (PaymentResponse payment : payments) {
            BillDetail bill = billMap.get(payment.getBillId());
            if (bill == null) continue;

            totals.merge(
                    bill.billCategory().name(),
                    payment.getAmount(),
                    BigDecimal::add
            );
        }

        return new CategorySpendSummary(
                userId,
                yearMonth,
                totals,
                ""
        );
    }
}
