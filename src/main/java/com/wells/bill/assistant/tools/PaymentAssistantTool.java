package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.model.*;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.PaymentService;
import com.wells.bill.assistant.store.ContextStoreInMemory;
import com.wells.bill.assistant.store.PaymentConfirmationStoreInMemory;
import com.wells.bill.assistant.util.IdempotencyKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAssistantTool {

    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(5);

    private final BillService billService;
    private final PaymentService paymentService;
    private final ContextStoreInMemory contextStore;
    private final PaymentConfirmationStoreInMemory confirmationStore;

    /* =====================================================
     * 1️⃣ REQUEST PAYMENT CONFIRMATION (READ-ONLY)
     * ===================================================== */

    @Tool(
            name = "paymentRequest",
            description = """
                    Request explicit user confirmation before paying a bill.
                    Confirmation expires automatically after 5 minutes.
                    This tool does NOT create a payment intent.
                    """
    )
    public PaymentConfirmation paymentRequest(
            @ToolParam(description = "Bill Provider Name") String billName,
            @ToolParam(description = "Schedule payment after N days (0 = immediate)") int scheduledAfterDays
    ) {
        Context context = contextStore.getExistingContext();
        UUID userId = context.userId();

        log.info("PaymentAssistantTool: Creating payment request for userId={}, billName={}, scheduledAfterDays={}",
                userId, billName, scheduledAfterDays
        );
        BillDetail bill = getDetails(userId, billName);

        if (bill.status() != BillStatus.VERIFIED) {
            throw new IllegalArgumentException("Bill is not ready for payment.");
        }

        LocalDate scheduledDate =
                scheduledAfterDays > 0
                        ? LocalDate.now().plusDays(scheduledAfterDays)
                        : null;

        String token = UUID.randomUUID().toString();

        confirmationStore.save(userId,
                new PaymentConfirmationToken(
                        token,
                        bill.id(),
                        userId,
                        scheduledDate,
                        Instant.now().plus(CONFIRMATION_TTL)
                )
        );

        log.info("Payment confirmation requested: billId={}, userId={}, scheduledDate={}, token={}",
                bill.id(), userId, scheduledDate, token
        );

        return new PaymentConfirmation(
                bill.id(),
                userId,
                bill.amountDue().amount(),
                bill.amountDue().currency().getSymbol(),
                scheduledDate,
                token,
                scheduledDate == null
                        ? "Please confirm payment of %s %s to %s within 5 minutes."
                        .formatted(
                                bill.amountDue(),
                                bill.amountDue().currency().getSymbol(),
                                bill.providerName()
                        )
                        : "Please confirm scheduled payment of %s %s to %s on %s within 5 minutes."
                        .formatted(
                                bill.amountDue(),
                                bill.amountDue().currency().getSymbol(),
                                bill.providerName(),
                                scheduledDate
                        )
        );
    }

    /* =====================================================
     * 2️⃣ CONFIRM & PAY BILL (STATE-CHANGING)
     * ===================================================== */

    @Tool(
            name = "confirmAndPayBill",
            description = """
                    Confirm and execute payment for a bill.
                    Requires a valid, unexpired confirmation token.
                    This tool creates a payment intent and marks the bill as PAID.
                    """
    )
    public String confirmAndPayBill(
            @ToolParam(description = "Confirmation token") String confirmationToken
    ) {
        Context context = contextStore.getExistingContext();
        UUID userId = context.userId();

        log.info("Confirming payment for userId={}, confirmationToken={}",
                userId, confirmationToken
        );

        PaymentConfirmationToken stored =
                confirmationStore.find(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Confirmation token expired or invalid."
                                )
                        );

        if (!stored.token().equals(confirmationToken) ||
                !stored.userId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Confirmation token does not match bill or user."
            );
        }

        // One-time use
        confirmationStore.delete(userId);

        BillDetail bill = billService.getBill(stored.billId(), userId);

        String idempotencyKey = IdempotencyKeyGenerator.generate(
                userId,
                bill.id(),
                bill.amountDue().amount(),
                bill.amountDue().currency().getSymbol()
        );

        PaymentIntentRequest req = new PaymentIntentRequest();
        req.setBillId(bill.id());
        req.setUserId(userId);
        req.setAmount(bill.amountDue().amount());
        req.setCurrency(bill.amountDue().currency().getSymbol());
        req.setScheduledDate(stored.scheduledDate());
        req.setIdempotencyKey(idempotencyKey);
        req.setExecutedBy(ExecutedBy.AI_SUGGESTED);

        log.info("Confirming and executing payment: billId={}, userId={}, amount={}, currency={}, scheduledDate={}",
                bill.id(), userId, bill.amountDue().amount(),
                bill.amountDue().currency().getSymbol(),
                stored.scheduledDate()
        );

        var intent = paymentService.createPaymentIntent(req);

        log.info(
                "Payment executed via AI tool: billId={}, paymentId={}, scheduledDate={}",
                intent.getBillId(), intent.getPaymentId(), intent.getScheduledDate()
        );

        return intent.getScheduledDate() == null
                ? "Payment completed successfully."
                : "Payment scheduled successfully for " + intent.getScheduledDate() + ".";
    }

    @Tool(
            name = "getPaymentDetails",
            description = """
                    Retrieve details of a payment using the payment identifier.
                    This is a read-only helper tool and does not modify any data.
                    """
    )
    public PaymentResponse getPaymentDetails(
            @ToolParam(description = "Payment identifier") UUID paymentId
    ) {
        return paymentService.getPaymentById(paymentId);
    }

    @Tool(
            name = "explainPaymentStatus",
            description = """
                    Explain the current status of a payment in plain English.
                    Useful for answering user questions about payment progress or failure.
                    """
    )
    public String explainPaymentStatus(
            @ToolParam(description = "Payment identifier") UUID paymentId
    ) {
        PaymentResponse payment = paymentService.getPaymentById(paymentId);

        return switch (payment.getStatus()) {

            case SUCCESS -> """
                    ✅ Payment successful.
                    
                    Amount: %s %s
                    Bill ID: %s
                    Executed at: %s
                    Gateway reference: %s
                    """
                    .formatted(
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getBillId(),
                            payment.getExecutedAt(),
                            payment.getGatewayReference()
                    );

            case APPROVAL_PENDING -> """
                    ⏳ Payment is currently pending.
                    
                    Amount: %s %s
                    Scheduled date: %s
                    Please wait for the payment to be processed.
                    """
                    .formatted(
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getScheduledDate()
                    );

            case FAILED -> """
                    ❌ Payment failed.
                    
                    Amount: %s %s
                    Reason: %s
                    You may retry the payment or choose a different method.
                    """
                    .formatted(
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getFailureReason()
                    );

            case CANCELLED -> """
                    ⚠️ Payment was cancelled.
                    
                    Amount: %s %s
                    Cancelled at: %s
                    """
                    .formatted(
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getCancelledAt()
                    );

            default -> """
                    ℹ️ Payment status: %s
                    
                    Amount: %s %s
                    Created at: %s
                    """
                    .formatted(
                            payment.getStatus(),
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getCreatedAt()
                    );
        };
    }

    @Tool(
            name = "listPaymentsForUser",
            description = """
                    List all payments made by a user.
                    Read-only helper tool for payment history.
                    """
    )
    public List<PaymentResponse> listPaymentsForUser() {
        Context context = contextStore.getExistingContext();
        UUID userId = context.userId();

        return paymentService.getPaymentsForUser(userId);
    }

    @Tool(
            name = "explainScheduledPayment",
            description = """
                    Explain details of a scheduled (future) payment in plain English.
                    Read-only helper tool.
                    """
    )
    public String explainScheduledPayment(
            @ToolParam(description = "Payment identifier") UUID paymentId
    ) {
        PaymentResponse payment = paymentService.getPaymentById(paymentId);

        if (payment.getScheduledDate() == null) {
            return "This payment is not scheduled. It was processed immediately.";
        }

        return """
                📅 Scheduled Payment Details
                
                Amount: %s %s
                Bill ID: %s
                Scheduled Date: %s
                Current Status: %s
                
                The payment will be automatically executed on the scheduled date
                unless it is cancelled before then.
                """
                .formatted(
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getBillId(),
                        payment.getScheduledDate(),
                        payment.getStatus()
                );
    }

    @Tool(
            name = "paymentAnomalyDetection",
            description = """
                    Detect whether a payment appears unusual based on amount,
                    scheduling, and failure signals.
                    Read-only AI helper tool.
                    """
    )
    public PaymentAnomalyReport paymentAnomalyDetection(
            @ToolParam(description = "Payment identifier") UUID paymentId
    ) {
        Context context = contextStore.getExistingContext();
        UUID userId = context.userId();

        PaymentResponse current = paymentService.getPaymentById(paymentId);
        List<PaymentResponse> allPayments = paymentService.getPaymentsForUser(userId);

        List<String> signals = new ArrayList<>();
        int score = 0;

        double avgAmount = allPayments.stream()
                .filter(p -> !p.getPaymentId().equals(paymentId))
                .mapToDouble(p -> p.getAmount().doubleValue())
                .average()
                .orElse(current.getAmount().doubleValue());

        /* -------------------------------
         * Amount anomaly
         * ------------------------------- */
        if (current.getAmount().doubleValue() > avgAmount * 1.5) {
            signals.add("Payment amount is significantly higher than usual.");
            score += 30;
        }

        /* -------------------------------
         * Failure signal
         * ------------------------------- */
        if (current.getFailureReason() != null) {
            signals.add("Payment has a failure reason recorded.");
            score += 25;
        }

        /* -------------------------------
         * Scheduled far in future
         * ------------------------------- */
        if (current.getScheduledDate() != null &&
                current.getScheduledDate().isAfter(LocalDate.now().plusMonths(3))) {
            signals.add("Payment is scheduled far in the future.");
            score += 15;
        }

        /* -------------------------------
         * Payment type anomaly
         * ------------------------------- */
        if (current.getPaymentType() == PaymentType.SCHEDULED &&
                current.getScheduledDate() == null) {
            signals.add("Payment marked as scheduled but has no scheduled date.");
            score += 20;
        }

        boolean anomalous = score >= 40;

        return new PaymentAnomalyReport(
                current.getPaymentId(),
                anomalous,
                Math.min(score, 100),
                current.getAmount(),
                BigDecimal.valueOf(avgAmount),
                signals,
                anomalous
                        ? "This payment looks unusual and may require review."
                        : "This payment looks normal."
        );
    }

    @Tool(
            name = "monthlyPaymentSummary",
            description = """
                    Generate a monthly summary of a user's payments.
                    Includes totals, success/failure breakdown, and upcoming scheduled payments.
                    Read-only AI helper tool.
                    """
    )
    public MonthlyPaymentSummary monthlyPaymentSummary(
            @ToolParam(description = "Month in YYYY-MM format") String month
    ) {
        Context context = contextStore.getExistingContext();
        UUID userId = context.userId();

        YearMonth yearMonth = YearMonth.parse(month);

        List<PaymentResponse> payments =
                paymentService.getPaymentsForUser(userId)
                        .stream()
                        .filter(p ->
                                p.getCreatedAt() != null &&
                                        YearMonth.from(p.getCreatedAt().atZone(ZoneId.systemDefault()))
                                                .equals(yearMonth)
                        )
                        .toList();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal successAmount = BigDecimal.ZERO;
        BigDecimal failedAmount = BigDecimal.ZERO;
        BigDecimal scheduledAmount = BigDecimal.ZERO;

        for (PaymentResponse p : payments) {
            totalAmount = totalAmount.add(p.getAmount());

            switch (p.getStatus()) {
                case SUCCESS -> successAmount = successAmount.add(p.getAmount());
                case FAILED -> failedAmount = failedAmount.add(p.getAmount());
                case APPROVAL_PENDING -> {
                    if (p.getScheduledDate() != null) {
                        scheduledAmount = scheduledAmount.add(p.getAmount());
                    }
                }
            }
        }

        List<String> breakdown = List.of(
                "Successful payments: " + successAmount,
                "Failed payments: " + failedAmount,
                "Scheduled payments: " + scheduledAmount
        );

        String summaryText = """
                Monthly Payment Summary for %s:
                
                - Total payments: %d
                - Total amount: %s
                - Successful: %s
                - Failed: %s
                - Scheduled: %s
                """
                .formatted(
                        yearMonth,
                        payments.size(),
                        totalAmount,
                        successAmount,
                        failedAmount,
                        scheduledAmount
                );

        return new MonthlyPaymentSummary(
                userId,
                yearMonth,
                payments.size(),
                totalAmount,
                successAmount,
                failedAmount,
                scheduledAmount,
                breakdown,
                summaryText.strip()
        );
    }

    @Tool(
            name = "explainMonthlyPaymentSummary",
            description = """
                    Explain the user's monthly payment summary in plain English.
                    Helpful for conversational responses.
                    """
    )
    public String explainMonthlyPaymentSummary(
            @ToolParam(description = "Month in YYYY-MM format") String month
    ) {
        MonthlyPaymentSummary summary = monthlyPaymentSummary(month);

        return """
                📊 Your payment summary for %s:
                
                You made %d payments totaling %s.
                %s was successfully paid.
                %s failed.
                %s is scheduled for future execution.
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

    @Tool(
            name = "detectSpendingTrend",
            description = """
                    Detect month-over-month spending trends for a user.
                    Identifies whether spending is increasing, decreasing, or stable.
                    Read-only AI helper tool.
                    """
    )
    public SpendingTrendReport detectSpendingTrend(
            @ToolParam(description = "Month in YYYY-MM format") String month
    ) {
        YearMonth current = YearMonth.parse(month);
        YearMonth previous = current.minusMonths(1);

        Context context = contextStore.getExistingContext();
        UUID userId = context.userId();

        List<PaymentResponse> payments =
                paymentService.getPaymentsForUser(userId);

        BigDecimal currentSpend = payments.stream()
                .filter(p -> p.getCreatedAt() != null)
                .filter(p -> YearMonth.from(
                        p.getCreatedAt().atZone(ZoneId.systemDefault())
                ).equals(current))
                .map(PaymentResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousSpend = payments.stream()
                .filter(p -> p.getCreatedAt() != null)
                .filter(p -> YearMonth.from(
                        p.getCreatedAt().atZone(ZoneId.systemDefault())
                ).equals(previous))
                .map(PaymentResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal changePct = BigDecimal.ZERO;
        List<String> signals = new ArrayList<>();

        if (previousSpend.compareTo(BigDecimal.ZERO) > 0) {
            changePct = currentSpend
                    .subtract(previousSpend)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousSpend, 2, RoundingMode.HALF_UP);
        }

        String trend;
        if (changePct.compareTo(BigDecimal.valueOf(10)) > 0) {
            trend = "INCREASING";
            signals.add("Spending increased significantly compared to last month.");
        } else if (changePct.compareTo(BigDecimal.valueOf(-10)) < 0) {
            trend = "DECREASING";
            signals.add("Spending decreased compared to last month.");
        } else {
            trend = "STABLE";
            signals.add("Spending is relatively stable month-over-month.");
        }

        String summary = """
                Spending trend for %s:
                - Current month spend: %s
                - Previous month spend: %s
                - Change: %s%%
                Trend: %s
                """
                .formatted(
                        current,
                        currentSpend,
                        previousSpend,
                        changePct,
                        trend
                );

        return new SpendingTrendReport(
                userId,
                current,
                currentSpend,
                previous,
                previousSpend,
                changePct,
                trend,
                signals,
                summary.strip()
        );
    }

    @Tool(
            name = "categoryWiseSpendSummary",
            description = """
                    Show category-wise spending (utilities, rent, etc.) for a given month.
                    Uses bill category mapping for accurate classification.
                    Read-only AI helper tool.
                    """
    )
    public CategorySpendSummary categoryWiseSpendSummary(
            @ToolParam(description = "Month in YYYY-MM format") String month
    ) {
        Context context = contextStore.getExistingContext();
        UUID userId = context.userId();

        YearMonth yearMonth = YearMonth.parse(month);
        Map<String, BigDecimal> categoryTotals = new HashMap<>();

        List<PaymentResponse> payments =
                paymentService.getPaymentsForUser(userId);

        for (PaymentResponse payment : payments) {
            if (payment.getCreatedAt() == null) continue;

            YearMonth paymentMonth =
                    YearMonth.from(
                            payment.getCreatedAt().atZone(ZoneId.systemDefault())
                    );

            if (!paymentMonth.equals(yearMonth)) continue;

            if (payment.getBillId() == null) continue;

            BillDetail bill = billService.getBill(payment.getBillId(), userId);
            String category = bill.billCategory().name();

            categoryTotals.merge(
                    category,
                    payment.getAmount(),
                    BigDecimal::add
            );
        }

        String summary = categoryTotals.isEmpty()
                ? "No payments found for the selected month."
                : "Category-wise spending calculated successfully.";

        return new CategorySpendSummary(
                userId,
                yearMonth,
                categoryTotals,
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
