package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.*;
import com.wells.bill.assistant.service.PaymentService;
import com.wells.bill.assistant.store.ContextStoreInMemory;
import com.wells.bill.assistant.util.IdempotencyKeyGenerator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;

import static com.wells.bill.assistant.util.CookieGenerator.CONTEXT_COOKIE;
import static com.wells.bill.assistant.util.CookieGenerator.getContextKey;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ContextStoreInMemory contextStore;

    // -----------------------------
    // Create Payment Intent
    // -----------------------------
    @PostMapping("/intent")
    public ResponseEntity<PaymentIntentResponse> createIntent(
            @Valid @RequestBody PaymentIntentRequest req,
            @CookieValue(value = CONTEXT_COOKIE, required = false) String contextKey,
            HttpServletResponse response) {

        // 1️⃣ Resolve key
        contextKey = getContextKey(contextKey, response);

        // 2️⃣ Load context (expires automatically after 10 min idle)
        Context context = contextStore.get(contextKey);

        req.setUserId(context.userId());
        String idempotencyKey = IdempotencyKeyGenerator.generate(
                req.getUserId(),
                req.getBillId(),
                req.getAmount(),
                req.getCurrency()
        );
        validateIdempotencyKey(idempotencyKey);
        PaymentIntentResponse paymentIntent = paymentService.createPaymentIntent(req);
        return ResponseEntity.ok(paymentIntent);
    }

    // -----------------------------
    // Execute Payment
    // -----------------------------
    @PostMapping("/{paymentId}/execute")
    public ResponseEntity<PaymentResponse> executePayment(
            @PathVariable UUID paymentId,
            @RequestBody ExecutePaymentRequest req,
            @CookieValue(value = CONTEXT_COOKIE, required = false) String contextKey,
            HttpServletResponse response) {

        // 1️⃣ Resolve key
        contextKey = getContextKey(contextKey, response);

        // 2️⃣ Load context (expires automatically after 10 min idle)
        Context context = contextStore.get(contextKey);

        req.setUserId(context.userId());
        req.setPaymentId(paymentId);
        PaymentResponse paymentResponse = paymentService.executePayment(req);
        return ResponseEntity.ok(paymentResponse);
    }

    // -----------------------------
    // Schedule Payment
    // -----------------------------
    @PostMapping("/schedule")
    public ResponseEntity<PaymentIntentResponse> schedulePayment(
            @RequestBody PaymentIntentRequest req,
            @CookieValue(value = CONTEXT_COOKIE, required = false) String contextKey,
            HttpServletResponse response) {

        // 1️⃣ Resolve key
        contextKey = getContextKey(contextKey, response);

        // 2️⃣ Load context (expires automatically after 10 min idle)
        Context context = contextStore.get(contextKey);

        req.setUserId(context.userId());
        if (req.getScheduledDate() == null) {
            throw new IllegalArgumentException("scheduledDate is required for scheduling");
        }
        String idempotencyKey = IdempotencyKeyGenerator.generate(
                req.getUserId(),
                req.getBillId(),
                req.getAmount(),
                req.getCurrency()
        );
        validateIdempotencyKey(idempotencyKey);
        PaymentIntentResponse paymentIntentResponse = paymentService.createPaymentIntent(req);
        return ResponseEntity.ok(paymentIntentResponse);
    }

    // ---------------------------------------
    // Cancel Scheduled Payment by Payment ID
    // ---------------------------------------
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<String> cancelScheduledPayment(@PathVariable UUID paymentId) {

        boolean ok = paymentService.cancelPayment(paymentId, ExecutedBy.USER);
        if (ok) return ResponseEntity.ok("Cancelled");
        return ResponseEntity.badRequest().body("Cannot cancel: either not found or not scheduled");
    }

    // ---------------------------------------
    // Request Approval by Payment ID
    // ---------------------------------------
    @PostMapping("/{paymentId}/requestApproval")
    public ResponseEntity<String> requestApproval(@PathVariable UUID paymentId) {

        paymentService.requestApproval(paymentId, ExecutedBy.USER);
        return ResponseEntity.ok("Requested");
    }

    // ---------------------------------------
    // Approve request by Payment ID
    // ---------------------------------------
    @PostMapping("/{paymentId}/approvePayment")
    public ResponseEntity<String> approvePayment(@PathVariable UUID paymentId) {
        paymentService.approvePayment(paymentId, ExecutedBy.USER);
        return ResponseEntity.ok("Approved");
    }

    // -----------------------------
    // Get Payment Status
    // -----------------------------
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        PaymentResponse paymentResponse = paymentService.getPaymentById(paymentId);
        if (paymentResponse != null) {
            return ResponseEntity.ok(paymentResponse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // -------------------------------
    // Force Scheduler Run (Dev only)
    // -------------------------------
    @PostMapping("/scheduler/run")
    public ResponseEntity<String> runSchedulerNow(@RequestBody ExecutePaymentRequest paymentRequest,
                                                  @CookieValue(value = CONTEXT_COOKIE, required = false) String contextKey,
                                                  HttpServletResponse response) {

        // 1️⃣ Resolve key
        contextKey = getContextKey(contextKey, response);

        // 2️⃣ Load context (expires automatically after 10 min idle)
        Context context = contextStore.get(contextKey);

        paymentRequest.setUserId(context.userId());
        paymentRequest.setExecutedBy(ExecutedBy.USER);

        paymentService.executeScheduledPayments(LocalDate.now(), paymentRequest);
        return ResponseEntity.ok("Scheduler executed");
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new InvalidUserInputException("Idempotency-Key header is required");
        }

        if (key.length() > 64) {
            throw new InvalidUserInputException("Idempotency-Key too long");
        }
    }

}
