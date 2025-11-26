package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.ScheduledPaymentEntity;
import com.wells.bill.assistant.model.PaymentScheduleStatus;
import com.wells.bill.assistant.repository.ScheduledPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentExecutionService {
    private static final Logger log = LoggerFactory.getLogger(PaymentExecutionService.class);

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final MakePaymentService makePaymentService;
    private final BillService billService;

    /**
     * Finds due scheduled payments and attempts to execute them in a transactional manner.
     * This method is public to make it testable; the Scheduled executor will call it.
     */
    @Transactional
    public void executeDuePayments(LocalDate asOfDate) {
        log.info("Executing scheduled payments due up to {}", asOfDate);
        List<ScheduledPaymentEntity> due = scheduledPaymentRepository.findAllByStatusAndScheduledDateLessThanEqual(PaymentScheduleStatus.SCHEDULED, asOfDate);
        for (ScheduledPaymentEntity sp : due) {
            try {
                // mark as processing to avoid double execution in concurrent runs
                sp.setStatus(PaymentScheduleStatus.PROCESSING);
                sp.setUpdatedAt(Instant.now());
                scheduledPaymentRepository.save(sp);

                // Build CreatePaymentRequest and call makePaymentService
                var req = new com.wells.bill.assistant.model.CreatePaymentRequest();
                req.setAmount(sp.getAmount());
                req.setCurrency(sp.getCurrency());
                // merchant/customer must be present in metadata or passed separately; using placeholders here
                req.setMerchantId(sp.getMerchantId());
                req.setCustomerId(sp.getCustomerId());

                String paymentId = makePaymentService.createPaymentRecord(req);

                // on success update scheduled payment
                sp.setPaymentId(UUID.fromString(paymentId.startsWith("pay_") ? paymentId.split("pay_")[1] : paymentId));
                sp.setStatus(PaymentScheduleStatus.COMPLETED);
                sp.setUpdatedAt(Instant.now());
                scheduledPaymentRepository.save(sp);

                try {
                    Long billIdLong = Long.valueOf(sp.getBillId());
                    billService.markAsPaid(billIdLong);
                    log.info("Bill marked as PAID after scheduled payment execution: billId={}", billIdLong);
                } catch (Exception ex) {
                    log.warn("Unable to mark bill as PAID: billId={}", sp.getBillId());
                }
                log.info("Successfully executed scheduled payment: {} -> paymentId={}", sp.getId(), paymentId);
            } catch (Exception ex) {
                log.error("Failed to execute scheduled payment {} : {}", sp.getId(), ex.getMessage(), ex);
                // mark as failed and continue
                sp.setStatus(PaymentScheduleStatus.FAILED);
                sp.setUpdatedAt(Instant.now());
                scheduledPaymentRepository.save(sp);
            }
        }
    }

    // scheduled to run every day at 2:00 AM (configurable)
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledExecutor() {
        executeDuePayments(LocalDate.now());
    }
}
