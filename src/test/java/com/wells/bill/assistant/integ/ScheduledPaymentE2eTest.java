package com.wells.bill.assistant.integ;

import com.wells.bill.assistant.entity.ScheduledPaymentEntity;
import com.wells.bill.assistant.model.PaymentScheduleStatus;
import com.wells.bill.assistant.repository.ScheduledPaymentRepository;
import com.wells.bill.assistant.service.PaymentExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
public class ScheduledPaymentE2eTest {

    @Autowired
    private ScheduledPaymentRepository scheduledPaymentRepository;

    @Autowired
    private PaymentExecutionService paymentExecutionService;

    @Test
    @Transactional
    void scheduledPayment_executesAndMarksCompleted() {
        ScheduledPaymentEntity sp = new ScheduledPaymentEntity();
        sp.setBillId("123");
        sp.setAmount(5000L);
        sp.setCurrency("usd");
        sp.setScheduledDate(LocalDate.now());
        sp.setStatus(PaymentScheduleStatus.SCHEDULED);
        scheduledPaymentRepository.save(sp);

        paymentExecutionService.executeDuePayments(LocalDate.now());

        var reloaded = scheduledPaymentRepository.findById(sp.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentScheduleStatus.COMPLETED);
    }
}
