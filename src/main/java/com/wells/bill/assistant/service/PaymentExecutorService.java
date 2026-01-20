package com.wells.bill.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.entity.PaymentStatus;
import com.wells.bill.assistant.gateway.PaymentGatewayClient;
import com.wells.bill.assistant.model.ExecutePaymentRequest;
import com.wells.bill.assistant.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.wells.bill.assistant.entity.PaymentStatus.SCHEDULED;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutorService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient gatewayClient;
    private final BillService billService;
    private final ObjectMapper objectMapper;

    // -------------------- Scheduler entry --------------------
    @Transactional
    public void executeDuePayments(LocalDate asOfDate) {
        log.info("[Scheduler] Executing scheduled payments due up to {}", asOfDate);

        List<PaymentEntity> duePayments = paymentRepository.findByStatusAndScheduledDateLessThanEqual(SCHEDULED, asOfDate);

        for (PaymentEntity payment : duePayments) {
            try {
                executeSinglePayment(payment, null);
            } catch (Exception ex) {
                log.error("Failed executing scheduled payment {}: {}", payment.getPaymentId(), ex.getMessage());
            }
        }
    }

    // -------------------- Core execution --------------------
    @Transactional
    public void executeSinglePayment(PaymentEntity payment, ExecutePaymentRequest req) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment already processing: " + payment.getPaymentId());
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        try {
            String cardToken = resolveCardToken(payment, req);

            PaymentGatewayClient.GatewayResponse resp = gatewayClient.charge(
                    payment.getAmount(),
                    payment.getCustomerId().toString(),
                    String.valueOf(payment.getBillId()),
                    payment.getIdempotencyKey(),
                    cardToken,
                    Map.of("paymentId", payment.getPaymentId())
            );

            if (resp != null && resp.success()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setGatewayReference(resp.reference());
                payment.setExecutedAt(Instant.now());

                billService.markPaid(payment.getBillId(), payment.getPaymentId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(resp == null ? "null gateway response" : resp.reference());
            }
        } catch (Exception ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            throw ex;
        }
    }

    // -------------------- Helpers --------------------
    private String resolveCardToken(PaymentEntity payment, ExecutePaymentRequest req) {
        if (req != null && req.getCardToken() != null && !req.getCardToken().isBlank()) {
            return req.getCardToken();
        }

        if (payment.getGatewayPayload() != null) {
            try {
                Map<?, ?> payload = objectMapper.readValue(payment.getGatewayPayload(), Map.class);
                Object token = payload.get("cardToken");
                if (token != null) {
                    return token.toString();
                }
            } catch (Exception e) {
                log.warn("Failed to parse gatewayPayload for payment {}", payment.getPaymentId());
            }
        }

        throw new IllegalArgumentException("Card token required to execute payment");
    }

    // -------------------- Scheduled trigger --------------------
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledExecutor() {
        executeDuePayments(LocalDate.now());
    }
}
