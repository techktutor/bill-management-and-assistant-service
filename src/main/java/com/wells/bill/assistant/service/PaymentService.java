package com.wells.bill.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.entity.PaymentStatus;
import com.wells.bill.assistant.entity.PaymentType;
import com.wells.bill.assistant.gateway.PaymentGatewayClient;
import com.wells.bill.assistant.model.CreatePaymentIntentRequest;
import com.wells.bill.assistant.model.ExecutePaymentRequest;
import com.wells.bill.assistant.model.PaymentIntentResponse;
import com.wells.bill.assistant.model.PaymentResponse;
import com.wells.bill.assistant.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient gatewayClient;
    private final BillService billService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------- Intent creation --------------------
    @Transactional
    public PaymentEntity createPaymentIntent(CreatePaymentIntentRequest req) {
        // idempotency: if provided, return existing
        if (req.getIdempotencyKey() != null) {
            Optional<PaymentEntity> existing = paymentRepository.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) return existing.get();
        }

        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setCustomerId(req.getCustomerId());
        paymentEntity.setBillId(req.getBillId());
        paymentEntity.setMerchantId(req.getMerchantId());
        paymentEntity.setAmount(req.getAmount());
        paymentEntity.setCurrency(req.getCurrency() == null ? "USD" : req.getCurrency());
        paymentEntity.setIdempotencyKey(req.getIdempotencyKey());
        paymentEntity.setPaymentType(req.getPaymentType() == null ? PaymentType.IMMEDIATE : req.getPaymentType());

        if (req.getPaymentType() == PaymentType.SCHEDULED || req.getScheduledDate() != null) {
            paymentEntity.setPaymentType(PaymentType.SCHEDULED);
            paymentEntity.setScheduledDate(req.getScheduledDate());
            paymentEntity.setStatus(PaymentStatus.SCHEDULED);
        } else {
            paymentEntity.setStatus(PaymentStatus.CREATED);
        }

        // NOTE: we do NOT accept cardToken here; card tokens should be provided when executing the payment.
        // For scheduled payments, callers should pass a cardToken (and we store it in gatewayPayload if they do).

        paymentRepository.save(paymentEntity);
        log.info("Created payment intent: {} bill={} type={} idem={}", paymentEntity.getPaymentId(), paymentEntity.getBillId(), paymentEntity.getPaymentType(), paymentEntity.getIdempotencyKey());
        return paymentEntity;
    }

    // -------------------- Execute payment --------------------
    @Transactional
    public PaymentEntity executePayment(String paymentId, ExecutePaymentRequest req) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment intent not found: " + paymentId));

        // Idempotent: if already successful just return
        if (payment.getStatus() == PaymentStatus.SUCCESS) return payment;

        // Prevent parallel execution
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment is already processing: " + paymentId);
        }

        // For scheduled payments, we may need a stored card token
        String cardToken = req.getCardToken();
        if (payment.getPaymentType() == PaymentType.SCHEDULED && (cardToken == null || cardToken.isBlank())) {
            // try to read from gatewayPayload (dev mode) - NOT recommended for production
            if (payment.getGatewayPayload() != null) {
                try {
                    Map<?, ?> payload = objectMapper.readValue(payment.getGatewayPayload(), Map.class);
                    Object t = payload.get("cardToken");
                    if (t != null) cardToken = t.toString();
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse gatewayPayload for stored token: {}", e.getMessage());
                }
            }
        }

        if (payment.getPaymentType() == PaymentType.SCHEDULED && (cardToken == null || cardToken.isBlank())) {
            throw new IllegalArgumentException("Scheduled payment requires a stored card token (or supply cardToken now)");
        }

        // mark processing
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        try {
            // Call gateway - use gateway idempotency key passed or fallback to payment idempotency
            String gatewayIdem = req.getGatewayIdempotencyKey() != null ? req.getGatewayIdempotencyKey() : payment.getIdempotencyKey();

            PaymentGatewayClient.GatewayResponse resp = gatewayClient.charge(
                    payment.getAmount(),
                    payment.getCustomerId().toString(),
                    String.valueOf(payment.getBillId()),
                    gatewayIdem,
                    cardToken,
                    Map.of("paymentId", payment.getPaymentId())
            );

            if (resp == null) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("null response from gateway");
            } else if (resp.success()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setGatewayReference(resp.reference());
                payment.setExecutedAt(Instant.now());
                // mark bill as paid
                try {
                    billService.markAsPaid(payment.getBillId(), payment.getPaymentId(), false);
                } catch (Exception ex) {
                    log.warn("Payment executed but failed to mark bill paid: {}", ex.getMessage());
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(resp.reference()); // gateway often returns error message; map accordingly
            }

        } catch (Exception ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            log.error("Error executing payment {}: {}", paymentId, ex.getMessage(), ex);
        }

        paymentRepository.save(payment);
        log.info("Payment {} execution finished with status={}", payment.getPaymentId(), payment.getStatus());
        return payment;
    }

    // -------------------- Scheduling Helpers --------------------
    @Transactional
    public PaymentEntity schedulePayment(Long billId, CreatePaymentIntentRequest req) {
        LocalDate scheduledDate = req.getScheduledDate();
        // reuse existing schedulePayment method signature: create intent + set scheduledDate
        return schedulePayment(billId, req, scheduledDate);
    }

    @Transactional
    public PaymentEntity schedulePayment(Long billId, CreatePaymentIntentRequest req, LocalDate scheduledDate) {
        // ensure idempotency key
        String idem = req.getIdempotencyKey();
        if (idem == null || idem.isBlank()) {
            idem = "sched-" + UUID.randomUUID();
            req.setIdempotencyKey(idem);
        }

        Optional<PaymentEntity> existing = paymentRepository.findByIdempotencyKey(idem);
        if (existing.isPresent()) return existing.get();

        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setCustomerId(req.getCustomerId());
        paymentEntity.setBillId(billId);
        paymentEntity.setMerchantId(req.getMerchantId());
        paymentEntity.setAmount(req.getAmount());
        paymentEntity.setCurrency(req.getCurrency() == null ? "USD" : req.getCurrency());
        paymentEntity.setIdempotencyKey(idem);
        paymentEntity.setPaymentType(PaymentType.SCHEDULED);
        paymentEntity.setScheduledDate(scheduledDate);
        paymentEntity.setStatus(PaymentStatus.SCHEDULED);

        paymentRepository.save(paymentEntity);
        log.info("Scheduled payment created: {} scheduledDate={} idem={}", paymentEntity.getPaymentId(), scheduledDate, idem);
        return paymentEntity;
    }

    @Transactional
    public boolean cancelScheduledPaymentByPaymentId(String paymentId) {
        Optional<PaymentEntity> maybe = paymentRepository.findByPaymentId(paymentId);
        if (maybe.isEmpty()) return false;
        PaymentEntity p = maybe.get();
        if (p.getPaymentType() != PaymentType.SCHEDULED) return false;
        if (p.getStatus() != PaymentStatus.SCHEDULED) return false;
        p.setStatus(PaymentStatus.CANCELLED);
        p.setCancelledAt(Instant.now());
        paymentRepository.save(p);
        log.info("Cancelled scheduled payment paymentId={}", paymentId);
        return true;
    }

    @Transactional
    public boolean cancelScheduledPayment(UUID id) {
        return paymentRepository.findById(id).map(p -> {
            if (p.getPaymentType() != PaymentType.SCHEDULED) return false;
            if (p.getStatus() != PaymentStatus.SCHEDULED) return false;
            p.setStatus(PaymentStatus.CANCELLED);
            p.setCancelledAt(Instant.now());
            paymentRepository.save(p);
            log.info("Cancelled scheduled payment id={}", id);
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<PaymentEntity> findDueScheduledPayments(LocalDate asOfDate) {
        return paymentRepository.findByStatusAndScheduledDateLessThanEqual(PaymentStatus.SCHEDULED, asOfDate);
    }

    @Transactional
    public void executeDueScheduledPayments(LocalDate asOfDate) {
        List<PaymentEntity> due = findDueScheduledPayments(asOfDate);
        for (PaymentEntity p : due) {
            try {
                String gatewayIdem = p.getIdempotencyKey() != null ? p.getIdempotencyKey() : ("sched-" + UUID.randomUUID());
                ExecutePaymentRequest execReq = new ExecutePaymentRequest();
                execReq.setPaymentId(p.getPaymentId());
                execReq.setGatewayIdempotencyKey(gatewayIdem);
                execReq.setExecutedBy("system-scheduler");
                // Try to extract stored cardToken from gatewayPayload (dev assumption)
                if (p.getGatewayPayload() != null) {
                    try {
                        Map<?, ?> mp = objectMapper.readValue(p.getGatewayPayload(), Map.class);
                        Object ct = mp.get("cardToken");
                        if (ct != null) execReq.setCardToken(ct.toString());
                    } catch (JsonProcessingException ignored) {
                    }
                }
                executePayment(p.getPaymentId(), execReq);
            } catch (Exception ex) {
                log.error("Error executing scheduled payment {}: {}", p.getPaymentId(), ex.getMessage());
            }
        }
    }

    // -------------------- Finders & mappers --------------------
    public Optional<PaymentEntity> findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    public List<PaymentEntity> findPaymentsByCustomer(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

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
