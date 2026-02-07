package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.PaymentResponse;
import com.wells.bill.assistant.service.PaymentService;
import com.wells.bill.assistant.util.ConversationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentQueryTool {

    private final PaymentService paymentService;

    /* =====================================================
     * 1️⃣ EXPLAIN LATEST PAYMENT STATUS (CHATBOT FRIENDLY)
     * ===================================================== */

    @Tool(
            name = "explainLatestPaymentStatus",
            description = """
                    Explain the status of the most recent payment made
                    to a bill provider in plain English.
                    """
    )
    public String explainLatestPaymentStatus(
            @ToolParam(description = "Bill provider name (example: Electricity, Rent)")
            String providerName
    ) {
        UUID userId = requireUser();

        PaymentResponse payment = getMostRecentPayment(providerName, userId);

        log.info("Explaining latest payment status for userId={}, provider={}, paymentId={}",
                userId, providerName, payment.getPaymentId());

        return switch (payment.getStatus()) {

            case SUCCESS -> """
                    ✅ Your most recent payment to %s was successful.
                    
                    Amount: %s %s
                    Executed at: %s
                    """
                    .formatted(
                            providerName,
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getExecutedAt()
                    );

            case APPROVAL_PENDING -> """
                    ⏳ Your payment to %s is still pending.
                    
                    Amount: %s %s
                    Scheduled Date: %s
                    """
                    .formatted(
                            providerName,
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getScheduledDate()
                    );

            case FAILED -> """
                    ❌ Your payment to %s failed.
                    
                    Amount: %s %s
                    Reason: %s
                    
                    You may retry the payment.
                    """
                    .formatted(
                            providerName,
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getFailureReason()
                    );

            case CANCELLED -> """
                    ⚠️ Your payment to %s was cancelled.
                    
                    Amount: %s %s
                    Cancelled at: %s
                    """
                    .formatted(
                            providerName,
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getCancelledAt()
                    );

            default -> """
                    ℹ️ Your latest payment to %s has status: %s
                    
                    Amount: %s %s
                    Created at: %s
                    """
                    .formatted(
                            providerName,
                            payment.getStatus(),
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getCreatedAt()
                    );
        };
    }

    /* =====================================================
     * 2️⃣ EXPLAIN RECENT PAYMENTS (CHATBOT FRIENDLY)
     * ===================================================== */

    @Tool(
            name = "explainRecentPayments",
            description = """
                    Summarize the user's most recent payments in plain English.
                    Useful when the user asks: "Show my recent payments".
                    """
    )
    public String explainRecentPayments(
            @ToolParam(description = "Number of recent payments to show (example: 5)")
            int limit
    ) {
        UUID userId = requireUser();

        log.info("Explaining recent payments for userId={}, limit={}", userId, limit);

        if (limit <= 0) {
            throw new InvalidUserInputException("Limit must be greater than 0.");
        }

        List<PaymentResponse> recentPayments = paymentService.getPaymentsForUser(userId).stream()
                        .filter(p -> p.getCreatedAt() != null)
                        .sorted(Comparator.comparing(PaymentResponse::getCreatedAt).reversed())
                        .limit(limit)
                        .toList();

        if (recentPayments.isEmpty()) {
            return "You haven’t made any payments yet.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🧾 Here are your most recent payments:\n\n");

        for (PaymentResponse p : recentPayments) {
            sb.append("• ")
                    .append(p.getAmount()).append(" ").append(p.getCurrency())
                    .append(" → Status: ").append(p.getStatus())
                    .append(" (").append(p.getCreatedAt()).append(")")
                    .append("\n");
        }

        sb.append("\nWant details for a specific bill provider?");

        return sb.toString();
    }

    /* =====================================================
     * INTERNAL HELPERS
     * ===================================================== */

    private UUID requireUser() {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            throw new InvalidUserInputException("No user context bound.");
        }
        return userId;
    }

    private PaymentResponse getMostRecentPayment(String providerName, UUID userId) {
        log.info("Fetching most recent payment for userId={}, providerName={}", userId, providerName);
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
}
