package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.model.CreatePaymentRequest;
import com.wells.bill.assistant.model.PaymentStatus;
import com.wells.bill.assistant.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Creates a payment record (used by immediate payments & scheduled payments).
     */
    public String createPaymentRecord(CreatePaymentRequest req) {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        payment.setPaymentId("pay_" + UUID.randomUUID());
        payment.setMerchantId(req.getMerchantId());
        payment.setCustomerId(req.getCustomerId());
        payment.setAmount(req.getAmount());
        payment.setCurrency(req.getCurrency());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        paymentRepository.save(payment);

        log.info("Payment record created: {}", payment.getPaymentId());
        return payment.getPaymentId();
    }

    /**
     * Save a payment entity directly (usually for audit logging).
     */
    public PaymentEntity recordPayment(PaymentEntity payment) {
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(Instant.now());
        }
        payment.setUpdatedAt(Instant.now());

        log.info("Recording payment audit: paymentId={} customerId={}",
                payment.getPaymentId(), payment.getCustomerId());

        return paymentRepository.save(payment);
    }

    /**
     * Retrieve payment history for a customer.
     */
    @Transactional(readOnly = true)
    public List<PaymentEntity> findPaymentsByCustomer(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    /**
     * Find a specific payment by its unique paymentId (not DB UUID).
     */
    @Transactional(readOnly = true)
    public Optional<PaymentEntity> findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
}
