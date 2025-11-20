package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.CreatePaymentRequest;
import com.wells.bill.assistant.entity.ScheduledPayment;
import com.wells.bill.assistant.repository.ScheduledPaymentRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentsToolAdapter {

    private final ScheduledPaymentService scheduledPaymentService;
    private final LocalPaymentService localPaymentService;

    public PaymentsToolAdapter(ScheduledPaymentService scheduledPaymentService, LocalPaymentService localPaymentService, ScheduledPaymentRepository scheduledPaymentRepository) {
        this.scheduledPaymentService = scheduledPaymentService;
        this.localPaymentService = localPaymentService;
    }

    // Instant payment tool
    @Tool(name = "payBill", description = "Make an immediate payment. Requires billId, amount, cardToken, merchantId, customerId.")
    public String payBill(@ToolParam(description = "Bill ID") String billId, @ToolParam(description = "Amount in USD") Double amount, @ToolParam(description = "Card Token") String cardToken, @ToolParam(description = "Merchant ID") String merchantId, @ToolParam(description = "Customer ID") String customerId) {
        try {
            long cents = (long) (amount * 100);
            CreatePaymentRequest req = new CreatePaymentRequest();
            req.setMerchantId(UUID.fromString(merchantId));
            req.setCustomerId(UUID.fromString(customerId));
            req.setAmount(cents);
            req.setCurrency("usd");
            req.setCardToken(cardToken);

            // authorize and capture
            var auth = localPaymentService.authorizeAndCapture(req);
            return "Payment initiated. Payment ID: " + auth.getPaymentId();
        } catch (Exception e) {
            return "Payment failed: " + e.getMessage();
        }
    }

    // Schedule payment tool
    @Tool(name = "schedulePayment", description = "Schedule a future bill payment. Requires billId, amount, ISO date (yyyy-MM-dd), cardToken, merchantId, customerId.")
    public String schedulePayment(@ToolParam(description = "Bill ID") String billId, @ToolParam(description = "Amount in USD") Double amount, @ToolParam(description = "Scheduled date in ISO format (yyyy-MM-dd)") String date, @ToolParam(description = "Card Token") String cardToken, @ToolParam(description = "Merchant ID") String merchantId, @ToolParam(description = "Customer ID") String customerId) {
        try {
            long cents = (long) (amount * 100);
            CreatePaymentRequest req = new CreatePaymentRequest();
            req.setMerchantId(UUID.fromString(merchantId));
            req.setCustomerId(UUID.fromString(customerId));
            req.setAmount(cents);
            req.setCurrency("usd");
            req.setCardToken(cardToken);
            LocalDate scheduledDate = LocalDate.parse(date);
            ScheduledPayment sp = scheduledPaymentService.schedule(billId, req, scheduledDate);
            return "Scheduled payment created. ScheduledPaymentId: " + sp.getId();
        } catch (Exception e) {
            return "Scheduling failed: " + e.getMessage();
        }
    }

    // Cancel scheduled payment tool
    @Tool(name = "cancelScheduledPayment", description = "Cancel a previously scheduled payment. Requires scheduledPaymentId.")
    public String cancelScheduledPayment(@ToolParam(description = "Scheduled Payment ID") String scheduledPaymentId) {
        try {
            UUID id = UUID.fromString(scheduledPaymentId);
            boolean ok = scheduledPaymentService.cancel(id);
            return ok ? "Canceled" : "Unable to cancel (not found or already processed)";
        } catch (Exception e) {
            return "Cancel failed: " + e.getMessage();
        }
    }

    // TOOL EXECUTION (CALLED BY AssistantOrchestratorService)
    public String executeTool(String toolName, Map<String, Object> args) {
        try {
            return switch (toolName) {
                case "payBill" -> payBill(args.get("billId").toString(), Double.valueOf(args.get("amount").toString()), args.get("cardToken").toString(), args.get("merchantId").toString(), args.get("customerId").toString());

                case "schedulePayment" -> schedulePayment(args.get("billId").toString(), Double.valueOf(args.get("amount").toString()), args.get("date").toString(), args.get("cardToken").toString(), args.get("merchantId").toString(), args.get("customerId").toString());

                case "cancelScheduledPayment" -> cancelScheduledPayment(args.get("scheduledPaymentId").toString());

                default -> "Unknown tool: " + toolName;
            };
        } catch (Exception e) {
            return "Error executing tool '" + toolName + "': " + e.getMessage();
        }
    }
}
