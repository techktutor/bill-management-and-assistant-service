package com.wells.bill.assistant.scheduler;

import com.wells.bill.assistant.model.ExecutePaymentRequest;
import com.wells.bill.assistant.model.ExecutedBy;
import com.wells.bill.assistant.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PaymentExecutor {

    private final PaymentService paymentService;

    // -------------------- Scheduled trigger --------------------
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledExecutor() {
        ExecutePaymentRequest req = new ExecutePaymentRequest();
        req.setExecutedBy(ExecutedBy.SYSTEM_SCHEDULER);
        paymentService.executeScheduledPayments(LocalDate.now(), req);
    }
}
