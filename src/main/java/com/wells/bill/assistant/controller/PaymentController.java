package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.model.CreatePaymentIntentRequest;
import com.wells.bill.assistant.model.ExecutePaymentRequest;
import com.wells.bill.assistant.model.PaymentIntentResponse;
import com.wells.bill.assistant.model.PaymentResponse;
import com.wells.bill.assistant.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // -----------------------------
    // Create Payment Intent
    // -----------------------------
    @PostMapping("/intent")
    public ResponseEntity<PaymentIntentResponse> createIntent(@RequestBody CreatePaymentIntentRequest req) {
        PaymentEntity entity = paymentService.createPaymentIntent(req);
        return ResponseEntity.ok(paymentService.toIntentResponse(entity));
    }

    // -----------------------------
    // Execute Payment
    // -----------------------------
    @PostMapping("/{paymentId}/execute")
    public ResponseEntity<PaymentResponse> executePayment(
            @PathVariable String paymentId,
            @RequestBody ExecutePaymentRequest req
    ) {
        req.setPaymentId(paymentId);
        PaymentEntity entity = paymentService.executePayment(paymentId, req);
        return ResponseEntity.ok(paymentService.toPaymentResponse(entity));
    }

    // -----------------------------
    // Schedule Payment
    // -----------------------------
    @PostMapping("/schedule")
    public ResponseEntity<PaymentIntentResponse> schedulePayment(@RequestBody CreatePaymentIntentRequest req) {
        if (req.getScheduledDate() == null) {
            throw new IllegalArgumentException("scheduledDate is required for scheduling");
        }
        PaymentEntity entity = paymentService.schedulePayment(req.getBillId(), req, req.getScheduledDate());
        return ResponseEntity.ok(paymentService.toIntentResponse(entity));
    }

    // -----------------------------
    // Cancel Scheduled Payment by Payment ID
    // -----------------------------
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<String> cancelScheduledPayment(@PathVariable String paymentId) {
        boolean ok = paymentService.cancelScheduledPaymentByPaymentId(paymentId);
        if (ok) return ResponseEntity.ok("Cancelled");
        return ResponseEntity.badRequest().body("Cannot cancel: either not found or not scheduled");
    }

    // -----------------------------
    // Get Payment Status
    // -----------------------------
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return paymentService.findByPaymentId(paymentId)
                .map(p -> ResponseEntity.ok(paymentService.toPaymentResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    // -----------------------------
    // Force Scheduler Run (Dev only)
    // -----------------------------
    @PostMapping("/scheduler/run")
    public ResponseEntity<String> runSchedulerNow() {
        paymentService.executeDueScheduledPayments(LocalDate.now());
        return ResponseEntity.ok("Scheduler executed");
    }
}
