package com.wells.bill.assistant.service;

import com.wells.bill.assistant.client.AcquirerClient;
import com.wells.bill.assistant.entity.*;
import com.wells.bill.assistant.repository.CardTokenRepository;
import com.wells.bill.assistant.repository.OutboxRepository;
import com.wells.bill.assistant.repository.PaymentRepository;
import com.wells.bill.assistant.util.JsonUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxDispatcher {

    private final OutboxRepository outboxRepository;
    private final PaymentRepository paymentRepository;
    private final AcquirerClient acquirerClient;
    private final LedgerService ledgerService;
    private final CardTokenRepository cardTokenRepo;

    public OutboxDispatcher(OutboxRepository outboxRepository,
                            PaymentRepository paymentRepository,
                            AcquirerClient acquirerClient,
                            LedgerService ledgerService,
                            CardTokenRepository cardTokenRepo) {
        this.outboxRepository = outboxRepository;
        this.paymentRepository = paymentRepository;
        this.acquirerClient = acquirerClient;
        this.ledgerService = ledgerService;
        this.cardTokenRepo = cardTokenRepo;
    }

    @Scheduled(fixedDelayString = "5000")
    @Transactional
    public void dispatch() {
        List<OutboxEvent> events = outboxRepository.findAll();
        for (OutboxEvent e : events) {
            if (e.isProcessed()) continue;
            try {
                if ("AUTHORIZE_PAYMENT".equals(e.getEventType())) {
                    AuthorizePayload p = JsonUtil.fromJson(e.getPayload(), AuthorizePayload.class);
                    CardToken token = cardTokenRepo.findByToken(p.cardToken()).orElseThrow();
                    AcquirerResponse ar = acquirerClient.authorize(p.cardToken(), p.amount(), p.currency(), p.paymentId());
                    var payment = paymentRepository.findById(UUID.fromString(p.paymentId())).orElseThrow();
                    if (ar.isSuccess()) {
                        payment.setStatus(PaymentStatus.AUTHORIZED);
                        payment.setPaymentIntentId(ar.getRemoteAuthId());
                        payment.setUpdatedAt(Instant.now());
                        paymentRepository.save(payment);
                        ledgerService.createReserveEntry(payment);
                    } else {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                    }
                } else if ("CAPTURE_PAYMENT".equals(e.getEventType())) {
                    CapturePayload cp = JsonUtil.fromJson(e.getPayload(), CapturePayload.class);
                    AcquirerResponse ar = acquirerClient.capture(cp.remoteAuthId(), cp.amount());
                    var payment = paymentRepository.findAll().stream()
                            .filter(p -> cp.remoteAuthId().equals(p.getPaymentIntentId()))
                            .findFirst().orElseThrow();
                    if (ar.isSuccess()) {
                        payment.setStatus(PaymentStatus.CAPTURED);
                        payment.setUpdatedAt(Instant.now());
                        paymentRepository.save(payment);
                        ledgerService.createCaptureEntry(payment);
                    }
                }
                e.setProcessed(true);
                outboxRepository.save(e);
            } catch (Exception ex) {
            }
        }
    }
}
