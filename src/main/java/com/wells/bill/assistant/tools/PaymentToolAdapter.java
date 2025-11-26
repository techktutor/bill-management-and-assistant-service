package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.entity.ScheduledPaymentEntity;
import com.wells.bill.assistant.model.CreatePaymentRequest;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.MakePaymentService;
import com.wells.bill.assistant.service.ScheduledPaymentService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@AllArgsConstructor
public class PaymentToolAdapter {

    private static final Logger log = LoggerFactory.getLogger(PaymentToolAdapter.class);
    private static final String DEFAULT_CURRENCY = "usd";
    private static final double MAX_AMOUNT = 1000000.0;

    private final BillService billService;
    private final MakePaymentService makePaymentService;
    private final ScheduledPaymentService scheduledPaymentService;

    @Tool(name = "payBill",
            description = "Make an immediate payment. Requires explicit confirmation. Parameters: billId, amount, cardToken, merchantId, customerId, confirm=true")
    public String payBill(
            @ToolParam(description = "Bill identifier") String billId,
            @ToolParam(description = "Amount in USD (e.g., 150.50)") Double amount,
            @ToolParam(description = "Tokenized card reference") String cardToken,
            @ToolParam(description = "Merchant UUID") String merchantId,
            @ToolParam(description = "Customer UUID") String customerId,
            @ToolParam(description = "User confirmation: must be true to execute payment") Boolean confirm) {

        try {
            if (!Boolean.TRUE.equals(confirm)) {
                return "Payment not executed. Please confirm with: confirm=true";
            }

            if (isInvalidInput(billId, amount, cardToken, merchantId, customerId)) {
                log.warn("Invalid payment inputs: billId={}, amount={}, merchantId={}, customerId={}",
                        billId, amount, merchantId, customerId);
                return "Payment failed: Invalid input parameters.";
            }

            if (amount > MAX_AMOUNT) {
                return "Payment failed: Amount exceeds system limit.";
            }

            if (!isValidToken(cardToken)) {
                return "Payment failed: Invalid card token format.";
            }

            long cents = toCents(amount);

            CreatePaymentRequest req = buildPaymentRequest(amount, merchantId, customerId);

            String paymentId = makePaymentService.createPaymentRecord(req);
            log.info("Instant payment created: paymentId={}, billId={}, amount=${}", paymentId, billId, amount);
            billService.markAsPaid(Long.valueOf(billId));
            return String.format("Payment successful. Payment ID: %s | Amount: $%.2f", paymentId, amount);

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in payment request: {}", e.getMessage());
            return "Payment failed: Invalid merchant or customer ID format.";
        } catch (Exception e) {
            log.error("Payment creation failed: billId={}, amount={}, error={}", billId, amount, e.getMessage());
            return "Payment failed: " + sanitize(e.getMessage());
        }
    }

    @Tool(name = "schedulePayment", description = "Schedule a future payment. Requires date in yyyy-MM-dd format.")
    public String schedulePayment(
            @ToolParam(description = "Bill identifier") String billId,
            @ToolParam(description = "Amount in USD (e.g., 150.50)") Double amount,
            @ToolParam(description = "Payment date in ISO format: yyyy-MM-dd") String date,
            @ToolParam(description = "Tokenized card reference") String cardToken,
            @ToolParam(description = "Merchant UUID") String merchantId,
            @ToolParam(description = "Customer UUID") String customerId,
            @ToolParam(description = "User confirmation: must be true to schedule payment") Boolean confirm) {

        try {
            if (!Boolean.TRUE.equals(confirm)) {
                return "Payment not scheduled. Please confirm with: confirm=true";
            }

            if (isInvalidInput(billId, amount, cardToken, merchantId, customerId)) {
                log.warn("Invalid scheduled payment inputs: billId={}, date={}, merchantId={}", billId, date, merchantId);
                return "Scheduling failed: Invalid input parameters.";
            }

            if (amount > MAX_AMOUNT) {
                return "Scheduling failed: Amount exceeds system limit.";
            }

            if (!isValidToken(cardToken)) {
                return "Scheduling failed: Invalid card token format.";
            }

            LocalDate scheduledDate;
            try {
                scheduledDate = LocalDate.parse(date);
                if (scheduledDate.isBefore(LocalDate.now())) {
                    log.warn("Scheduled date is in the past: {}", date);
                    return "Scheduling failed: Date must be in the future.";
                }
            } catch (Exception e) {
                log.error("Invalid date format: {}", date);
                return "Scheduling failed: Invalid date format (yyyy-MM-dd).";
            }

            CreatePaymentRequest req = buildPaymentRequest(amount, merchantId, customerId);
            ScheduledPaymentEntity sp = scheduledPaymentService.schedule(billId, req, scheduledDate);

            log.info("Scheduled payment created: id={}, billId={}, date={}, amount=${}", sp.getId(), billId, date, amount);
            return String.format(
                    "Payment scheduled. Scheduled Payment ID: %s | Date: %s | Amount: $%.2f",
                    sp.getId(), date, amount);

        } catch (Exception e) {
            log.error("Payment scheduling failed: billId={}, date={}, error={}", billId, date, e.getMessage());
            return "Scheduling failed: " + sanitize(e.getMessage());
        }
    }

    @Tool(name = "cancelScheduledPayment",
            description = "Cancel a scheduled payment by its UUID.")
    public String cancelScheduledPayment(
            @ToolParam(description = "Scheduled Payment UUID") String scheduledPaymentId,
            @ToolParam(description = "User confirmation: must be true to cancel payment") Boolean confirm) {

        try {
            if (!Boolean.TRUE.equals(confirm)) {
                return "Cancel not executed. Please confirm with: confirm=true";
            }

            if (scheduledPaymentId == null || scheduledPaymentId.isBlank()) {
                log.warn("Empty scheduled payment ID provided");
                return "Cancel failed: Scheduled Payment ID is required.";
            }

            UUID id = UUID.fromString(scheduledPaymentId);
            boolean cancelled = scheduledPaymentService.cancel(id);

            if (cancelled) {
                log.info("Scheduled payment cancelled: {}", id);
                return String.format("Scheduled payment %s has been cancelled.", id);
            }

            log.warn("Cancellation failed: {} not found or already processed", id);
            return "Cancel failed: Payment not found or already processed.";

        } catch (IllegalArgumentException e) {
            log.error("Invalid scheduled payment ID: {}", scheduledPaymentId);
            return "Cancel failed: Invalid Scheduled Payment ID (must be UUID).";
        } catch (Exception e) {
            log.error("Cancel operation failed for {}: {}", scheduledPaymentId, e.getMessage());
            return "Cancel failed: " + sanitize(e.getMessage());
        }
    }

    // ---------------------- Helper Methods ----------------------

    private boolean isInvalidInput(String billId, Double amount, String cardToken, String merchantId, String customerId) {
        return billId == null || billId.isBlank() ||
                amount == null || amount <= 0 ||
                cardToken == null || cardToken.isBlank() ||
                merchantId == null || merchantId.isBlank() ||
                customerId == null || customerId.isBlank();
    }

    private boolean isValidToken(String token) {
        return token != null && token.matches("^(tok|card)_[A-Za-z0-9_]+$");
    }

    private long toCents(Double amount) {
        return (long) (amount * 100);
    }

    private CreatePaymentRequest buildPaymentRequest(Double amount, String merchantId, String customerId) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setMerchantId(UUID.fromString(merchantId));
        req.setCustomerId(UUID.fromString(customerId));
        req.setAmount(toCents(amount));
        req.setCurrency(DEFAULT_CURRENCY);
        return req;
    }

    private String sanitize(String message) {
        return message == null ? "Unknown error" : message.replaceAll("[\\[\\]]", " ");
    }
}
