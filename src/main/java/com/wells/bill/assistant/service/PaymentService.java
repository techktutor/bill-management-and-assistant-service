package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.entity.PaymentStatus;
import com.wells.bill.assistant.entity.PaymentType;
import com.wells.bill.assistant.exception.DuplicatePaymentException;
import com.wells.bill.assistant.exception.DuplicateScheduleException;
import com.wells.bill.assistant.model.*;
import com.wells.bill.assistant.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified payment service: intents, execution, scheduling and cancellation.
 * <p>
 * Notes:
 * - This implementation stores a cardToken (if provided) inside gatewayPayload as JSON for demo/dev only.
 * In production, store tokens in a dedicated, encrypted vault or use a token reference from your gateway.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BillService billService;
    private final PaymentRepository paymentRepository;
    private final PaymentExecutorService paymentExecutorService;

    // -------------------- Intent creation --------------------
    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest req) {
        if (req.getIdempotencyKey() != null) {
            paymentRepository.findByIdempotencyKey(req.getIdempotencyKey())
                    .ifPresent(p -> {
                        throw new DuplicatePaymentException("Payment already exists for idempotencyKey");
                    });
        }

        // 🔐 Validate bill before creating intent
        BillSummary bill = billService.getBillSummary(req.getBillId());
        if (bill.status() != BillStatus.VERIFIED) {
            throw new IllegalStateException("Bill not ready for payment");
        }

        if (req.getAmount().compareTo(bill.amount()) != 0) {
            throw new IllegalArgumentException("Payment amount must match bill amount");
        }

        PaymentEntity payment = getPaymentEntity(req, bill);

        PaymentEntity saved = paymentRepository.save(payment);
        log.info("Payment intent created: {} bill={}", saved.getPaymentId(), saved.getBillId());
        return toIntentResponse(saved);
    }

    private static PaymentEntity getPaymentEntity(PaymentIntentRequest req, BillSummary bill) {
        PaymentEntity payment = new PaymentEntity();
        payment.setCustomerId(req.getCustomerId());
        payment.setBillId(req.getBillId());
        payment.setAmount(req.getAmount());
        payment.setCurrency(req.getCurrency() == null ? bill.currency() : req.getCurrency());
        payment.setIdempotencyKey(req.getIdempotencyKey());
        payment.setPaymentType(req.getPaymentType() == null ? PaymentType.IMMEDIATE : req.getPaymentType());
        payment.setApprovalSource(req.getApprovalSource());

        if (payment.getPaymentType() == PaymentType.SCHEDULED) {
            payment.setScheduledDate(req.getScheduledDate());
            payment.setStatus(PaymentStatus.SCHEDULED);
        } else {
            payment.setStatus(PaymentStatus.CREATED);
        }
        return payment;
    }

    // -------------------- Execution delegation --------------------
    @Transactional
    public PaymentResponse executePayment(ExecutePaymentRequest req) {
        PaymentEntity payment = paymentRepository.findByPaymentId(req.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + req.getPaymentId()));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toPaymentResponse(payment);
        }

        paymentExecutorService.executeSinglePayment(payment, req);
        return toPaymentResponse(payment);
    }

    @Transactional
    public void executeDueScheduledPayments(LocalDate asOfDate) {
        List<PaymentEntity> due = findDueScheduledPayments(asOfDate);
        for (PaymentEntity p : due) {
            try {
                paymentExecutorService.executeSinglePayment(p, null);
            } catch (Exception e) {
                log.error("Error executing scheduled payment paymentId={}", p.getPaymentId(), e);
            }
        }
    }

    public List<PaymentEntity> findDueScheduledPayments(LocalDate asOfDate) {
        return paymentRepository.findByPaymentTypeAndStatusAndScheduledDateLessThanEqual(
                PaymentType.SCHEDULED,
                PaymentStatus.SCHEDULED,
                asOfDate
        );
    }

    // -------------------- Scheduling helpers --------------------
    @Transactional
    public PaymentIntentResponse schedulePayment(UUID billId, PaymentIntentRequest req, LocalDate scheduledDate) {
        if (req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank()) {
            req.setIdempotencyKey("sched-" + UUID.randomUUID());
        }

        paymentRepository.findByIdempotencyKey(req.getIdempotencyKey())
                .ifPresent(p -> {
                    throw new DuplicateScheduleException("Scheduled payment already exists");
                });

        BillSummary bill = billService.getBillSummary(billId);
        if (bill.status() != BillStatus.VERIFIED) {
            throw new IllegalStateException("Bill not ready for scheduled payment");
        }

        PaymentEntity payment = new PaymentEntity();
        payment.setCustomerId(req.getCustomerId());
        payment.setBillId(billId);
        payment.setAmount(bill.amount());
        payment.setCurrency(bill.currency());
        payment.setIdempotencyKey(req.getIdempotencyKey());
        payment.setPaymentType(PaymentType.SCHEDULED);
        payment.setScheduledDate(scheduledDate);
        payment.setStatus(PaymentStatus.SCHEDULED);
        payment.setApprovalSource(req.getApprovalSource());

        PaymentEntity saved = paymentRepository.save(payment);
        log.info("Scheduled payment created: {} scheduledDate={}", saved.getPaymentId(), scheduledDate);
        return toIntentResponse(saved);
    }

    @Transactional
    public boolean cancelScheduledPaymentByPaymentId(String paymentId) {
        Optional<PaymentEntity> maybe = paymentRepository.findByPaymentId(paymentId);
        if (maybe.isEmpty()) {
            return false;
        }
        PaymentEntity p = maybe.get();
        if (p.getPaymentType() != PaymentType.SCHEDULED) {
            return false;
        }
        if (p.getStatus() != PaymentStatus.SCHEDULED) {
            return false;
        }
        p.setStatus(PaymentStatus.CANCELLED);
        p.setCancelledAt(Instant.now());
        paymentRepository.save(p);
        log.info("Cancelled scheduled payment paymentId={}", paymentId);
        return true;
    }

    public Optional<PaymentEntity> findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    // -------------------- Mappers --------------------
    public PaymentIntentResponse toIntentResponse(PaymentEntity p) {
        PaymentIntentResponse r = new PaymentIntentResponse();
        r.setPaymentId(p.getPaymentId());
        r.setIdempotencyKey(p.getIdempotencyKey());
        r.setStatus(p.getStatus());
        r.setPaymentType(p.getPaymentType());
        r.setAmount(p.getAmount());
        r.setCurrency(p.getCurrency());
        r.setCustomerId(p.getCustomerId());
        r.setBillId(p.getBillId());
        r.setScheduledDate(p.getScheduledDate());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    public PaymentResponse toPaymentResponse(PaymentEntity p) {
        PaymentResponse r = new PaymentResponse();
        r.setPaymentId(p.getPaymentId());
        r.setStatus(p.getStatus());
        r.setPaymentType(p.getPaymentType());
        r.setAmount(p.getAmount());
        r.setCurrency(p.getCurrency());
        r.setCustomerId(p.getCustomerId());
        r.setBillId(p.getBillId());
        r.setScheduledDate(p.getScheduledDate());
        r.setExecutedAt(p.getExecutedAt());
        r.setCancelledAt(p.getCancelledAt());
        r.setGatewayReference(p.getGatewayReference());
        r.setFailureReason(p.getFailureReason());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
