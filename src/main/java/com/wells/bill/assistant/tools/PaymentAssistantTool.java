package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.BillSummary;
import com.wells.bill.assistant.model.PaymentIntentRequest;
import com.wells.bill.assistant.model.PaymentIntentResponse;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class PaymentAssistantTool {

    private static final String DEFAULT_CURRENCY = "USD";

    private final BillService billService;
    private final PaymentService paymentService;

    @Tool(name = "createPaymentIntent", description = "Create a payment intent for a bill. Does NOT execute payment. Requires confirmation.")
    public String createPaymentIntent(
            @ToolParam(description = "Bill ID") String billId,
            @ToolParam(description = "Merchant UUID") String merchantId,
            @ToolParam(description = "Customer UUID") String customerId,
            @ToolParam(description = "User confirmation (must be true)") Boolean confirm) {

        if (!Boolean.TRUE.equals(confirm)) {
            return "Payment intent not created. Please confirm with confirm=true.";
        }

        try {
            UUID billUUID = UUID.fromString(billId);
            UUID merchantUUID = UUID.fromString(merchantId);
            UUID customerUUID = UUID.fromString(customerId);

            BillSummary bill = billService.getBill(billUUID);

            if (bill.status() != BillStatus.PAYMENT_READY) {
                return "Bill is not ready for payment.";
            }

            PaymentIntentRequest req = new PaymentIntentRequest();
            req.setBillId(billUUID);
            req.setMerchantId(merchantUUID);
            req.setCustomerId(customerUUID);
            req.setAmount(bill.amount());
            req.setCurrency(DEFAULT_CURRENCY);

            PaymentIntentResponse intent = paymentService.createPaymentIntent(req);

            log.info("Payment intent created via AI tool: paymentId={} billId={}", intent.getPaymentId(), billId);
            return String.format("Payment intent created successfully. Payment ID: %s. Please confirm execution.", intent.getPaymentId());
        } catch (IllegalArgumentException e) {
            return "Invalid UUID provided.";
        } catch (Exception e) {
            log.error("Failed to create payment intent via AI tool", e);
            return "Failed to create payment intent: " + e.getMessage();
        }
    }

    @Tool(name = "schedulePaymentIntent", description = "Schedule a payment intent for a future date. Does NOT execute payment.")
    public String schedulePaymentIntent(
            @ToolParam(description = "Bill ID") String billId,
            @ToolParam(description = "Merchant UUID") String merchantId,
            @ToolParam(description = "Customer UUID") String customerId,
            @ToolParam(description = "Scheduled date (yyyy-MM-dd)") String date,
            @ToolParam(description = "User confirmation (must be true)") Boolean confirm) {

        if (!Boolean.TRUE.equals(confirm)) {
            return "Payment not scheduled. Please confirm with confirm=true.";
        }

        try {
            UUID billUUID = UUID.fromString(billId);
            UUID merchantUUID = UUID.fromString(merchantId);
            UUID customerUUID = UUID.fromString(customerId);
            LocalDate scheduledDate = LocalDate.parse(date);

            if (scheduledDate.isBefore(LocalDate.now())) {
                return "Scheduled date must be in the future.";
            }

            BillSummary bill = billService.getBill(billUUID);
            if (bill.status() != BillStatus.PAYMENT_READY) {
                return "Bill is not ready for scheduled payment.";
            }

            PaymentIntentRequest req = new PaymentIntentRequest();
            req.setMerchantId(merchantUUID);
            req.setCustomerId(customerUUID);
            req.setAmount(bill.amount());
            req.setCurrency(DEFAULT_CURRENCY);

            PaymentIntentResponse intent = paymentService.schedulePayment(billUUID, req, scheduledDate);

            log.info("Scheduled payment intent created via AI tool: paymentId={} date={}", intent.getPaymentId(), date);
            return String.format("Payment scheduled successfully. Payment ID: %s on %s.", intent.getPaymentId(), date);
        } catch (Exception exception) {
            log.error("Failed to schedule payment intent via AI tool", exception);
            return "Failed to schedule payment: " + exception.getMessage();
        }
    }
}
