package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.entity.PaymentStatus;
import com.wells.bill.assistant.entity.PaymentType;
import com.wells.bill.assistant.exception.DuplicatePaymentException;
import com.wells.bill.assistant.model.*;
import com.wells.bill.assistant.repository.PaymentRepository;
import com.wells.bill.assistant.util.PaymentStateMachine;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final BillService billService;
    private final PaymentRepository paymentRepository;
    private final PaymentExecutorService paymentExecutorService;

    /**
     * Step 1: Create payment intent
     */
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest req) {
        if (req.getIdempotencyKey() != null) {
            paymentRepository.findByIdempotencyKey(req.getIdempotencyKey())
                    .ifPresent(p -> {
                        log.info("Idempotent payment request: key={}, paymentId={}, status={}",
                                p.getIdempotencyKey(),
                                p.getId(),
                                p.getStatus()
                        );

                        throw new DuplicatePaymentException("Payment already exists for idempotencyKey");
                    });
        }

        // 🔐 Validate bill before creating intent
        BillDetail bill = billService.getBill(req.getBillId());
        if (bill.status() != BillStatus.VERIFIED) {
            throw new IllegalStateException("Bill not ready for payment");
        }

        if (req.getAmount().compareTo(bill.amountDue()) != 0) {
            throw new IllegalArgumentException("Payment amount must match bill amount");
        }

        PaymentEntity payment = createPaymentEntity(req, bill);

        PaymentEntity saved = paymentRepository.save(payment);
        log.info("Payment intent created with paymentId= {} for billId= {}", saved.getId(), saved.getBillId());
        return toIntentResponse(saved);
    }

    private static PaymentEntity createPaymentEntity(PaymentIntentRequest req, BillDetail bill) {
        PaymentEntity payment = new PaymentEntity();
        payment.setUserId(req.getUserId());
        payment.setBillId(req.getBillId());
        payment.setAmount(req.getAmount());
        payment.setCurrency(req.getCurrency() == null ? bill.currency() : req.getCurrency());
        payment.setIdempotencyKey(req.getIdempotencyKey());

        if (req.getExecutedBy() == null) {
            throw new IllegalArgumentException("ExecutedBy is required to execute payment");
        }
        payment.setExecutedBy(req.getExecutedBy());

        if (req.getScheduledDate() != null) {
            payment.setScheduledDate(req.getScheduledDate());
            payment.setStatus(PaymentStatus.SCHEDULED);
            payment.setPaymentType(PaymentType.SCHEDULED);
        } else {
            payment.setStatus(PaymentStatus.CREATED);
            payment.setPaymentType(PaymentType.IMMEDIATE);
        }
        return payment;
    }

    /**
     * Step 2: Request approval (manual / auto)
     */
    public void requestApproval(UUID paymentId, ExecutedBy executedBy) {
        if (executedBy == null) {
            throw new IllegalArgumentException("ExecutedBy is required to execute payment");
        }
        PaymentEntity payment = getPayment(paymentId);
        transition(payment, PaymentStatus.APPROVAL_PENDING);
    }

    /**
     * Step 3: Approve payment
     */
    public void approvePayment(UUID paymentId, ExecutedBy executedBy) {
        if (executedBy == null) {
            throw new IllegalArgumentException("ExecutedBy is required to execute payment");
        }
        PaymentEntity payment = getPayment(paymentId);

        transition(payment, PaymentStatus.APPROVED);

        payment.setExecutedBy(executedBy);
        payment.setApprovedAt(Instant.now());
    }

    /**
     * Cancel payment
     */
    public boolean cancelPayment(UUID paymentId, ExecutedBy executedBy) {
        if (executedBy == null) {
            throw new IllegalArgumentException("ExecutedBy is required to execute payment");
        }
        PaymentEntity payment = getPayment(paymentId);

        transition(payment, PaymentStatus.CANCELLED);
        payment.setCancelledAt(Instant.now());
        return Boolean.TRUE;
    }

    public PaymentResponse getPaymentById(UUID id) {
        PaymentEntity payment = getPayment(id);
        return toPaymentResponse(payment);
    }

    // -------------------- Execution --------------------
    public PaymentResponse executePayment(ExecutePaymentRequest req) {
        PaymentEntity payment = getPayment(req.getPaymentId());
        return executeSinglePayment(payment, req);
    }

    public PaymentResponse executeSinglePayment(PaymentEntity payment, ExecutePaymentRequest req) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Current payment status is already SUCCESS");
        }

        if (req.getExecutedBy() == null) {
            throw new IllegalArgumentException("ExecutedBy is required to execute payment");
        }

        transition(payment, PaymentStatus.PROCESSING);
        payment.setUserId(req.getUserId());
        payment.setExecutedBy(req.getExecutedBy());
        paymentRepository.save(payment);
        try {
            GatewayResponse resp = paymentExecutorService.executeSinglePayment(payment);
            if (resp != null && resp.success()) {
                markSuccess(payment, resp.referenceId());
                billService.markPaid(payment.getBillId(), payment.getId());
            } else {
                assert resp != null;
                String reason = resp.referenceId();
                if (reason == null || reason.isBlank()) {
                    reason = "Unknown failure";
                }
                markFailure(payment, reason);
            }
        } catch (Exception ex) {
            log.error("Error executing payment paymentId= {}", payment.getId(), ex);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            throw ex;
        }
        return toPaymentResponse(payment);
    }

    public void executeScheduledPayments(LocalDate asOfDate, ExecutePaymentRequest executePaymentRequest) {
        List<PaymentEntity> scheduledPayments = findDueScheduledPayments(asOfDate);
        for (PaymentEntity payment : scheduledPayments) {
            executeSinglePayment(payment, executePaymentRequest);
        }
    }

    public List<PaymentEntity> findDueScheduledPayments(LocalDate asOfDate) {
        return paymentRepository.findByPaymentTypeAndStatusAndScheduledDateLessThanEqual(
                PaymentType.SCHEDULED,
                PaymentStatus.SCHEDULED,
                asOfDate
        );
    }

    // ------------------------
    // Helpers
    // ------------------------
    private PaymentEntity getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Payment not found: " + id)
                );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForUser(UUID userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    private void markSuccess(PaymentEntity payment, String gatewayRef) {
        transition(payment, PaymentStatus.SUCCESS);
        payment.setGatewayReferenceId(gatewayRef);
        payment.setExecutedAt(Instant.now());
    }

    private void markFailure(PaymentEntity payment, String reason) {
        transition(payment, PaymentStatus.FAILED);
        payment.setFailureReason(reason);
    }

    private void transition(PaymentEntity payment, PaymentStatus next) {
        PaymentStateMachine.validateTransition(
                payment.getStatus(),
                next
        );
        payment.setStatus(next);
    }

    // -------------------- Mappers --------------------
    private PaymentIntentResponse toIntentResponse(PaymentEntity p) {
        PaymentIntentResponse r = new PaymentIntentResponse();
        r.setPaymentId(p.getId());
        r.setIdempotencyKey(p.getIdempotencyKey());
        r.setStatus(p.getStatus());
        r.setPaymentType(p.getPaymentType());
        r.setAmount(p.getAmount());
        r.setCurrency(p.getCurrency());
        r.setCustomerId(p.getUserId());
        r.setBillId(p.getBillId());
        r.setScheduledDate(p.getScheduledDate());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    private PaymentResponse toPaymentResponse(PaymentEntity p) {
        PaymentResponse r = new PaymentResponse();
        r.setPaymentId(p.getId());
        r.setStatus(p.getStatus());
        r.setPaymentType(p.getPaymentType());
        r.setAmount(p.getAmount());
        r.setCurrency(p.getCurrency());
        r.setCustomerId(p.getUserId());
        r.setBillId(p.getBillId());
        r.setScheduledDate(p.getScheduledDate());
        r.setExecutedAt(p.getExecutedAt());
        r.setCancelledAt(p.getCancelledAt());
        r.setGatewayReference(p.getGatewayReferenceId());
        r.setFailureReason(p.getFailureReason());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
