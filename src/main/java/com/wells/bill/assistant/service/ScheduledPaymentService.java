package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.CreatePaymentRequest;
import com.wells.bill.assistant.entity.CreatePaymentResponse;
import com.wells.bill.assistant.entity.PaymentScheduleStatus;
import com.wells.bill.assistant.entity.ScheduledPayment;
import com.wells.bill.assistant.repository.ScheduledPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final LocalPaymentService localPaymentService;

    public ScheduledPaymentService(ScheduledPaymentRepository scheduledPaymentRepository,
                                   LocalPaymentService localPaymentService) {
        this.scheduledPaymentRepository = scheduledPaymentRepository;
        this.localPaymentService = localPaymentService;
    }

    public ScheduledPayment schedule(String billId, CreatePaymentRequest req, LocalDate date) {
        // authorize now (so funds are held) and schedule capture for date
        CreatePaymentResponse auth = localPaymentService.authorizePayment(req, UUID.randomUUID().toString());

        ScheduledPayment sp = new ScheduledPayment();
        sp.setId(UUID.randomUUID());
        sp.setBillId(billId);
        sp.setPaymentId(auth.getPaymentId());
        sp.setAmount(req.getAmount());
        sp.setCurrency(req.getCurrency());
        sp.setScheduledDate(date);
        sp.setStatus(PaymentScheduleStatus.SCHEDULED);
        sp.setCreatedAt(Instant.now());
        scheduledPaymentRepository.save(sp);
        return sp;
    }

    public boolean cancel(UUID scheduledPaymentId) {
        var spOpt = scheduledPaymentRepository.findById(scheduledPaymentId);
        if (spOpt.isEmpty()) return false;
        var sp = spOpt.get();
        if (sp.getStatus() != PaymentScheduleStatus.SCHEDULED) return false;
        sp.setStatus(PaymentScheduleStatus.CANCELED);
        sp.setUpdatedAt(Instant.now());
        scheduledPaymentRepository.save(sp);
        // Note: this does not currently void an authorization with the acquirer. Implement void if supported.
        return true;
    }
}
