package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.ScheduledPaymentEntity;
import com.wells.bill.assistant.model.CreatePaymentRequest;
import com.wells.bill.assistant.model.PaymentScheduleStatus;
import com.wells.bill.assistant.repository.ScheduledPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class ScheduledPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPaymentService.class);

    private final ScheduledPaymentRepository scheduledPaymentRepository;

    public ScheduledPaymentService(ScheduledPaymentRepository scheduledPaymentRepository) {
        this.scheduledPaymentRepository = scheduledPaymentRepository;
    }

    public ScheduledPaymentEntity schedule(String billId, CreatePaymentRequest req, LocalDate date) {
        ScheduledPaymentEntity sp = new ScheduledPaymentEntity();
        sp.setId(UUID.randomUUID());
        sp.setBillId(billId);
        // assigned only when executed
        sp.setPaymentId(null);
        sp.setAmount(req.getAmount());
        sp.setCurrency(req.getCurrency());
        sp.setScheduledDate(date);
        sp.setStatus(PaymentScheduleStatus.SCHEDULED);
        sp.setCreatedAt(Instant.now());
        sp.setUpdatedAt(Instant.now());

        UUID id = scheduledPaymentRepository.save(sp).getId();
        log.info("Scheduled payment created: {}", id);
        return sp;
    }

    public boolean cancel(UUID scheduledPaymentId) {
        return scheduledPaymentRepository.findById(scheduledPaymentId)
                .filter(sp -> sp.getStatus() == PaymentScheduleStatus.SCHEDULED)
                .map(sp -> {
                    sp.setStatus(PaymentScheduleStatus.CANCELED);
                    sp.setUpdatedAt(Instant.now());
                    scheduledPaymentRepository.save(sp);
                    log.info("Scheduled payment canceled: {}", scheduledPaymentId);
                    return true;
                })
                .orElse(false);
    }
}