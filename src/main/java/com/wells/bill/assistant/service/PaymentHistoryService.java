package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PaymentHistoryService.class);

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<PaymentEntity> findPaymentsByCustomer(java.util.UUID customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    @Transactional
    public PaymentEntity recordPayment(PaymentEntity payment) {
        if (payment.getCreatedAt() == null) payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        log.info("Recording payment for audit: paymentId={} customerId={}", payment.getPaymentId(), payment.getCustomerId());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentEntity> findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
}
