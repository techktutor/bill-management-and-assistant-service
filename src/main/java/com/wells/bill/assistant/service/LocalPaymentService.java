package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.CaptureResponse;
import com.wells.bill.assistant.entity.CreatePaymentRequest;
import com.wells.bill.assistant.entity.CreatePaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class LocalPaymentService {

    private final PaymentServiceIntegration paymentIntegration;

    public LocalPaymentService(PaymentServiceIntegration paymentIntegration) {
        this.paymentIntegration = paymentIntegration;
    }

    /**
     * Authorize a payment (creates payment record and performs async authorize via outbox).
     */
    public CreatePaymentResponse authorizePayment(CreatePaymentRequest req, String idempotencyKey) {
        return paymentIntegration.createPayment(req, idempotencyKey);
    }

    /**
     * Capture an already authorized payment.
     */
    public CaptureResponse capturePayment(UUID paymentId, Long amount, String idempotencyKey) {
        return paymentIntegration.capture(paymentId, amount, idempotencyKey);
    }

    /**
     * Quick helper: authorize then capture immediately (synchronous pattern using outbox optimistic flow)
     */
    public CreatePaymentResponse authorizeAndCapture(CreatePaymentRequest req) {
        // Authorize
        String idempAuth = UUID.randomUUID().toString();
        var authResp = authorizePayment(req, idempAuth);

        // Immediately create capture outbox event via PaymentServiceIntegration.capture (which writes outbox)
        try {
            String idempCapture = UUID.randomUUID().toString();
            // capture amount equal to request.amount
            paymentIntegration.capture(authResp.getPaymentId(), req.getAmount(), idempCapture);
        } catch (Exception ex) {
            // best-effort: capture is async; caller should check scheduled payment status later
        }
        return authResp;
    }
}

