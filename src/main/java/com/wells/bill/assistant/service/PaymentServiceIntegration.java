package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.*;
import com.wells.bill.assistant.repository.CardTokenRepository;
import com.wells.bill.assistant.repository.IdempotencyRepository;
import com.wells.bill.assistant.repository.OutboxRepository;
import com.wells.bill.assistant.repository.PaymentRepository;
import com.wells.bill.assistant.util.JsonUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceIntegration {

    private final PaymentRepository paymentRepo;
    private final IdempotencyRepository idempotencyRepo;
    private final LedgerService ledgerService;
    private final CardTokenRepository cardTokenRepo;
    private final OutboxRepository outboxRepository;

    public PaymentServiceIntegration(PaymentRepository paymentRepo,
                                     IdempotencyRepository idempotencyRepo,
                                     LedgerService ledgerService,
                                     CardTokenRepository cardTokenRepo,
                                     OutboxRepository outboxRepository) {
        this.paymentRepo = paymentRepo;
        this.idempotencyRepo = idempotencyRepo;
        this.ledgerService = ledgerService;
        this.cardTokenRepo = cardTokenRepo;
        this.outboxRepository = outboxRepository;
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest req, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = idempotencyRepo.findById(idempotencyKey);
            if (existing.isPresent() && existing.get().getExpiresAt().isAfter(Instant.now())) {
                return JsonUtil.fromJson(existing.get().getResponseSnapshot(), CreatePaymentResponse.class);
            }
        }

        // Validate token
        Optional<CardToken> tokenOpt = cardTokenRepo.findByToken(req.getCardToken());
        if (tokenOpt.isEmpty()) throw new IllegalArgumentException("card token not found");
        CardToken token = tokenOpt.get();

        // Create Payment record
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setMerchantId(req.getMerchantId());
        p.setCustomerId(req.getCustomerId());
        p.setAmount(req.getAmount());
        p.setCurrency(req.getCurrency());
        p.setStatus(PaymentStatus.CREATED);
        p.setCreatedAt(Instant.now());
        paymentRepo.save(p);

        // Instead of calling acquirer synchronously, write an outbox event so dispatcher will call acquirer
        OutboxEvent out = new OutboxEvent();
        out.setId(UUID.randomUUID());
        out.setAggregateId(p.getId());
        out.setEventType("AUTHORIZE_PAYMENT");
        out.setPayload(JsonUtil.toJson(new AuthorizePayload(req.getCardToken(), req.getAmount(), req.getCurrency(), p.getId().toString())));
        out.setProcessed(false);
        out.setCreatedAt(Instant.now());
        outboxRepository.save(out);

        // Reserve ledger entry locally
        ledgerService.createReserveEntry(p);

        CreatePaymentResponse resp = new CreatePaymentResponse(p.getId(), null, p.getStatus());
        if (idempotencyKey != null) {
            IdempotencyKey ik = new IdempotencyKey();
            ik.setKey(idempotencyKey);
            ik.setResponseSnapshot(JsonUtil.toJson(resp));
            ik.setCreatedAt(Instant.now());
            ik.setExpiresAt(Instant.now().plusSeconds(3600));
            idempotencyRepo.save(ik);
        }
        return resp;
    }

    public CaptureResponse capture(UUID paymentId, Long amount, String idempotencyKey) {
        var payment = paymentRepo.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("payment not found"));
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment not authorized");
        }

        // create outbox event for capture
        OutboxEvent out = new OutboxEvent();
        out.setId(UUID.randomUUID());
        out.setAggregateId(payment.getId());
        out.setEventType("CAPTURE_PAYMENT");
        out.setPayload(JsonUtil.toJson(new CapturePayload(payment.getPaymentIntentId(), amount)));
        out.setProcessed(false);
        out.setCreatedAt(Instant.now());
        outboxRepository.save(out);

        // Note: capture will be performed by dispatcher; optimistic response
        return new CaptureResponse(payment.getId(), PaymentStatus.AUTHORIZED);
    }
}
