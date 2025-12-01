package com.wells.bill.assistant.executor;

import com.wells.bill.assistant.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutionService {

    private final PaymentService paymentService;

    /**
     * Executes all scheduled payments due up to the given date.
     * Delegates to PaymentService.executeDueScheduledPayments.
     */
    @Transactional
    public void executeDuePayments(LocalDate asOfDate) {
        log.info("[Scheduler] Executing scheduled payments due up to {}", asOfDate);
        paymentService.executeDueScheduledPayments(asOfDate);
    }

    /**
     * Daily scheduled task at 2:00 AM server time.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledExecutor() {
        executeDuePayments(LocalDate.now());
    }
}
