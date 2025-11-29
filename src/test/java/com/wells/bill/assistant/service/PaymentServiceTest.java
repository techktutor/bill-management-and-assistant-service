package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.model.CreatePaymentRequest;
import com.wells.bill.assistant.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository);
    }

    @Test
    void createPaymentRecord_savesAndReturnsPaymentId() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setMerchantId(UUID.randomUUID());
        req.setCustomerId(UUID.randomUUID());
        req.setAmount(15000L);
        req.setCurrency("usd");

        PaymentEntity saved = new PaymentEntity();
        saved.setId(UUID.randomUUID());
        saved.setPaymentId("pay_test");
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(saved);

        String paymentId = service.createPaymentRecord(req);

        assertThat(paymentId).isEqualTo("pay_test");
        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        PaymentEntity captured = captor.getValue();
        assertThat(captured.getAmount()).isEqualTo(15000L);
        assertThat(captured.getCurrency()).isEqualTo("usd");
    }
}
