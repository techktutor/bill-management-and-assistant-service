package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.model.CreatePaymentRequest;
import com.wells.bill.assistant.service.BillManagementService;
import com.wells.bill.assistant.service.PaymentService;
import com.wells.bill.assistant.service.ScheduledPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAssistantToolTest {

    @Mock
    private ScheduledPaymentService scheduledPaymentService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private BillManagementService billService;

    private PaymentAssistantTool adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentAssistantTool(billService, paymentService, scheduledPaymentService);
    }

    @Test
    void payBill_withoutConfirm_returnsNotExecuted() {
        String res = adapter.payBill("bill-1", 10.0, "tok_abc123", UUID.randomUUID().toString(), UUID.randomUUID().toString(), false);
        assertThat(res).contains("not executed");
        verifyNoInteractions(paymentService);
    }

    @Test
    void payBill_invalidToken_returnsError() {
        String res = adapter.payBill("bill-1", 10.0, "invalidtoken", UUID.randomUUID().toString(), UUID.randomUUID().toString(), true);
        assertThat(res).contains("Invalid card token");
        verifyNoInteractions(paymentService);
    }

    @Test
    void payBill_success_callsMakePaymentService() {
        when(paymentService.createPaymentRecord(any(CreatePaymentRequest.class))).thenReturn("pay_12345");
        doNothing().when(billService).markAsPaid(anyLong());

        String merchant = UUID.randomUUID().toString();
        String customer = UUID.randomUUID().toString();

        String res = adapter.payBill("123456777", 20.0, "tok_card_123", merchant, customer, true);

        assertThat(res).contains("Payment successful");
        ArgumentCaptor<CreatePaymentRequest> captor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentService, times(1)).createPaymentRecord(captor.capture());
        CreatePaymentRequest captured = captor.getValue();
        assertThat(captured.getAmount()).isEqualTo(2000L);
        assertThat(captured.getCurrency()).isEqualTo("USD");
    }

    @Test
    void schedulePayment_withoutConfirm_returnsNotScheduled() {
        String res = adapter.schedulePayment("bill-1", 50.0, "2025-01-01", "tok_card_123", UUID.randomUUID().toString(), UUID.randomUUID().toString(), false);
        assertThat(res).contains("not scheduled");
        verifyNoInteractions(scheduledPaymentService);
    }

    @Test
    void cancelScheduledPayment_success() {
        UUID id = UUID.randomUUID();
        when(scheduledPaymentService.cancel(id)).thenReturn(true);

        String res = adapter.cancelScheduledPayment(id.toString(), true);
        assertThat(res).contains("has been cancelled");
        verify(scheduledPaymentService, times(1)).cancel(id);
    }

    @Test
    void cancelScheduledPayment_invalidUuid_returnsError() {
        String res = adapter.cancelScheduledPayment("not-a-uuid", true);
        assertThat(res).contains("Invalid Scheduled Payment ID");
    }
}
