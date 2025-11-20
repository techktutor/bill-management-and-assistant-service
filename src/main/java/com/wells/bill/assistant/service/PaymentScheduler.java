package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.PaymentScheduleStatus;
import com.wells.bill.assistant.entity.PaymentStatus;
import com.wells.bill.assistant.entity.ScheduledPayment;
import com.wells.bill.assistant.repository.ScheduledPaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class PaymentScheduler {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final LocalPaymentService localPaymentService;

    public PaymentScheduler(ScheduledPaymentRepository scheduledPaymentRepository,
                            LocalPaymentService localPaymentService) {
        this.scheduledPaymentRepository = scheduledPaymentRepository;
        this.localPaymentService = localPaymentService;
    }

    // Run once a day at midnight Asia/Kolkata to capture payments scheduled for today
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now();
        List<ScheduledPayment> list = scheduledPaymentRepository.findByScheduledDateAndStatus(today, PaymentScheduleStatus.SCHEDULED);
        for (ScheduledPayment sp : list) {
            try {
                // Attempt capture
                var captureResp = localPaymentService.capturePayment(sp.getPaymentId(), sp.getAmount(), java.util.UUID.randomUUID().toString());
                sp.setStatus(captureResp.getStatus() == PaymentStatus.CAPTURED ? PaymentScheduleStatus.EXECUTED : PaymentScheduleStatus.FAILED);
                sp.setUpdatedAt(java.time.Instant.now());
                scheduledPaymentRepository.save(sp);
            } catch (Exception ex) {
                sp.setStatus(PaymentScheduleStatus.FAILED);
                sp.setUpdatedAt(java.time.Instant.now());
                scheduledPaymentRepository.save(sp);
            }
        }
    }
}
