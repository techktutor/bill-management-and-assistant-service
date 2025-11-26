package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.ScheduledPaymentEntity;
import com.wells.bill.assistant.model.CreatePaymentRequest;
import com.wells.bill.assistant.model.PaymentScheduleStatus;
import com.wells.bill.assistant.repository.ScheduledPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledPaymentServiceTest {

    @Mock
    private ScheduledPaymentRepository repo;

    private ScheduledPaymentService service;

    @BeforeEach
    void setUp() {
        service = new ScheduledPaymentService(repo);
    }

    @Test
    void schedule_createsScheduledEntity() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setAmount(2000L);
        req.setCurrency("usd");

        ScheduledPaymentEntity saved = new ScheduledPaymentEntity();
        saved.setId(UUID.randomUUID());
        when(repo.save(any())).thenReturn(saved);

        ScheduledPaymentEntity sp = service.schedule("bill-123", req, LocalDate.now().plusDays(1));

        assertThat(sp).isNotNull();
        verify(repo, times(1)).save(any(ScheduledPaymentEntity.class));
    }

    @Test
    void cancel_existingScheduled_setsCanceled() {
        UUID id = UUID.randomUUID();
        ScheduledPaymentEntity existing = new ScheduledPaymentEntity();
        existing.setId(id);
        existing.setStatus(PaymentScheduleStatus.SCHEDULED);

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        boolean res = service.cancel(id);

        assertThat(res).isTrue();
        assertThat(existing.getStatus()).isEqualTo(PaymentScheduleStatus.CANCELED);
        verify(repo, times(1)).save(existing);
    }
}
