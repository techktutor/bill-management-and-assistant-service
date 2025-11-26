package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.model.CreatePaymentRequest;
import com.wells.bill.assistant.model.PaymentStatus;
import com.wells.bill.assistant.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class MakePaymentService {

    private static final Logger log = LoggerFactory.getLogger(MakePaymentService.class);

    private final PaymentRepository paymentRepository;

    public MakePaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

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

        String paymentId = paymentRepository.save(payment).getPaymentId();
        log.info("Payment record created: {}", paymentId);
        return paymentId;
    }
}