package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.*;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.CustomerService;
import com.wells.bill.assistant.service.SendEmailService;
import com.wells.bill.assistant.service.PaymentService;
import com.wells.bill.assistant.store.PaymentConfirmationStoreInMemory;
import com.wells.bill.assistant.util.ConversationContextHolder;
import com.wells.bill.assistant.util.IdempotencyKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntentTool {

    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(5);
    private static final int MAX_SCHEDULE_DAYS = 365;

    private final BillService billService;
    private final PaymentService paymentService;
    private final CustomerService customerService;
    private final SendEmailService sendEmailService;
    private final PaymentConfirmationStoreInMemory confirmationStore;

    /* =====================================================
     * 1️⃣ REQUEST CONFIRMATION (READ-ONLY)
     * ===================================================== */

    @Tool(
            name = "paymentIntentRequest",
            description = """
                    Request explicit user confirmation before paying a bill.
                    A confirmation code is sent via email and expires in 5 minutes.
                    This tool does NOT create a payment intent.
                    """
    )
    public PaymentConfirmation paymentRequest(
            @ToolParam(description = "Bill provider name (example: Electricity, Rent)") String billName,
            @ToolParam(description = "Schedule payment after N days (0 = immediate)") int scheduledAfterDays
    ) {
        UUID userId = requireUser();

        log.info("Payment intent request received for userId={}, billName={}, scheduledAfterDays={}",
                userId, billName, scheduledAfterDays);

        // ✅ Chatbot-safe scheduling handling (NO exceptions)
        if (scheduledAfterDays < 0) {
            scheduledAfterDays = 0;
        }

        if (scheduledAfterDays > MAX_SCHEDULE_DAYS) {
            return new PaymentConfirmation(
                    null,
                    userId,
                    null,
                    null,
                    null,
                    null,
                    "⚠️ Payments cannot be scheduled more than 1 year in advance. Please choose a shorter time."
            );
        }

        BillDetail bill = getBill(userId, billName);

        // Bill must be verified
        if (bill.status() != BillStatus.VERIFIED) {
            return new PaymentConfirmation(
                    bill.id(),
                    userId,
                    bill.amountDue().amount(),
                    bill.amountDue().currency().getSymbol(),
                    null,
                    null,
                    "⚠️ This bill is not verified yet, so payment cannot be processed."
            );
        }

        // Compute scheduled date (null = immediate)
        LocalDate scheduledDate =
                scheduledAfterDays > 0
                        ? LocalDate.now().plusDays(scheduledAfterDays)
                        : null;

        // ✅ Generate OTP token (6-digit)
        String token = generateOtp();

        // Store token server-side (1 active token per user)
        confirmationStore.save(userId,
                new PaymentConfirmationToken(
                        token,
                        bill.id(),
                        userId,
                        scheduledDate,
                        Instant.now().plus(CONFIRMATION_TTL)
                )
        );

        // Fetch user email
        String userEmail = customerService.getCustomerEmailByUserId(userId);

        if (userEmail == null || userEmail.isBlank()) {
            return new PaymentConfirmation(
                    bill.id(),
                    userId,
                    bill.amountDue().amount(),
                    bill.amountDue().currency().getSymbol(),
                    scheduledDate,
                    null,
                    "⚠️ No email is registered for your account. Payment confirmation cannot be sent."
            );
        }

        // Send OTP via email (LLM never sees token)
        sendEmailService.sendPaymentConfirmationTokenEmail(
                bill.providerName(),
                bill.amountDue().amount(),
                scheduledDate,
                token,
                userEmail
        );

        log.info("Payment confirmation email sent for userId={}, billId={}",
                userId, bill.id());

        return new PaymentConfirmation(
                bill.id(),
                userId,
                bill.amountDue().amount(),
                bill.amountDue().currency().getSymbol(),
                scheduledDate,
                null,
                """
                        ✅ A confirmation code has been sent to your registered email.
                        
                        Please paste the code here within 5 minutes to approve this payment.
                        """
        );
    }

    /* =====================================================
     * 2️⃣ CONFIRM & CREATE PAYMENT INTENT (STATE-CHANGING)
     * ===================================================== */

    @Tool(
            name = "confirmAndPayBill",
            description = """
                    Confirm and create a payment intent for a bill.
                    Requires the confirmation code received via email.
                    This tool creates a payment intent only.
                    """
    )
    public String confirmAndPayBill(
            @ToolParam(description = "Confirmation code received via email")
            String confirmationToken
    ) {
        UUID userId = requireUser();

        log.info("Payment confirmation received for userId={}", userId);

        PaymentConfirmationToken stored = confirmationStore.find(userId).orElse(null);

        // Token validation
        if (stored == null ||
                !stored.token().equals(confirmationToken) ||
                !stored.userId().equals(userId)) {
            return "❌ Confirmation code is invalid or does not match.";
        }

        // Expiry enforcement
        if (stored.expiresAt().isBefore(Instant.now())) {
            confirmationStore.delete(userId);
            return "⏳ Confirmation expired. Please request payment approval again.";
        }

        BillDetail bill = billService.getBill(stored.billId(), userId);

        // Idempotency protection
        String idempotencyKey = IdempotencyKeyGenerator.generate(
                userId,
                bill.id(),
                bill.amountDue().amount(),
                bill.amountDue().currency().getSymbol()
        );

        // Create payment intent
        PaymentIntentRequest req = new PaymentIntentRequest();
        req.setBillId(bill.id());
        req.setUserId(userId);
        req.setAmount(bill.amountDue().amount());
        req.setCurrency(bill.amountDue().currency().getCurrencyCode());
        req.setScheduledDate(stored.scheduledDate());
        req.setIdempotencyKey(idempotencyKey);
        req.setExecutedBy(ExecutedBy.AI_SUGGESTED);

        try {
            paymentService.createPaymentIntent(req);
        } catch (Exception e) {
            return String.format("❌ Failed to create payment intent: %s", e.getMessage());
        }

        // One-time token cleanup
        confirmationStore.delete(userId);

        log.info("Payment intent created successfully for userId={}, billId={}",
                userId, bill.id());

        return stored.scheduledDate() == null
                ? "✅ Payment intent created successfully."
                : "✅ Payment scheduled successfully for " + stored.scheduledDate() + ".";
    }

    @Tool(
            name = "resendPaymentConfirmationCode",
            description = """
                Resend the active payment confirmation code to the user's registered email.
                Works only if there is a pending, unexpired confirmation request.
                """
    )
    public String resendPaymentConfirmationCode() {

        UUID userId = requireUser();

        log.info("Resend payment confirmation code request for userId={}", userId);

        PaymentConfirmationToken stored = confirmationStore.find(userId).orElse(null);

        if (stored == null) {
            return """
                ⚠️ There is no pending payment confirmation request.

                Please start a new payment request first.
                """;
        }

        // Expired token → cleanup
        if (stored.expiresAt().isBefore(Instant.now())) {
            confirmationStore.delete(userId);
            return """
                ⏳ Your confirmation code has expired.

                Please request payment approval again.
                """;
        }

        // Fetch bill details
        BillDetail bill = billService.getBill(stored.billId(), userId);

        // Fetch user email
        String userEmail = customerService.getCustomerEmailByUserId(userId);

        if (userEmail == null || userEmail.isBlank()) {
            return "⚠️ No email is registered for your account. Cannot resend confirmation code.";
        }

        // Resend SAME token (no regeneration)
        sendEmailService.sendPaymentConfirmationTokenEmail(
                bill.providerName(),
                bill.amountDue().amount(),
                stored.scheduledDate(),
                stored.token(),
                userEmail
        );

        log.info("Resent payment confirmation code for userId={}, billId={}",
                userId, bill.id());

        return """
            ✅ Confirmation code resent successfully.

            Please check your email and paste the code here within 5 minutes to approve the payment.
            """;
    }

    @Tool(
            name = "cancelPendingPaymentRequest",
            description = """
                Cancel the currently pending payment confirmation request.
                This will invalidate the confirmation code immediately.
                """
    )
    public String cancelPendingPaymentRequest() {

        UUID userId = requireUser();

        log.info("Cancel pending payment request for userId={}", userId);

        PaymentConfirmationToken stored = confirmationStore.find(userId).orElse(null);

        if (stored == null) {
            return """
                ⚠️ There is no pending payment request to cancel.
                """;
        }

        confirmationStore.delete(userId);

        log.info("Cancelled pending payment request for userId={}, billId={}",
                userId, stored.billId());

        return """
            ✅ Pending payment request cancelled successfully.

            No payment will be processed unless you start a new request.
            """;
    }

    /* =====================================================
     * INTERNAL HELPERS
     * ===================================================== */

    private UUID requireUser() {
        UUID userId = ConversationContextHolder.getUserId();
        if (userId == null) {
            // System failure → exception is correct here
            throw new InvalidUserInputException("No user context bound.");
        }
        return userId;
    }

    private BillDetail getBill(UUID userId, String providerName) {
        List<BillDetail> bills = billService.findBillsByProviderName(userId, providerName);

        if (bills.isEmpty()) {
            // Chatbot-friendly response instead of exception
            throw new InvalidUserInputException(
                    "No bill found for provider: " + providerName
            );
        }

        return bills.getFirst();
    }

    /**
     * ✅ Generate a simple 6-digit OTP confirmation code
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(random.nextInt(900000) + 100000);
    }
}
