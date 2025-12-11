package com.wells.bill.assistant.integ;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.model.ExecutePaymentRequest;
import com.wells.bill.assistant.model.PaymentIntentRequest;
import com.wells.bill.assistant.repository.BillRepository;
import com.wells.bill.assistant.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BillRepository billRepository;

    private UUID customerId;
    private UUID billId;

    @BeforeEach
    void setup() {
        customerId = UUID.randomUUID();
        billId = UUID.randomUUID();
        paymentRepository.deleteAll();
    }

    // -----------------------------------------------------------
    // 1) CREATE PAYMENT INTENT
    // -----------------------------------------------------------
    @Test
    void testCreatePaymentIntent() throws Exception {
        PaymentIntentRequest req = new PaymentIntentRequest();
        req.setCustomerId(customerId);
        req.setBillId(billId);
        req.setMerchantId(UUID.randomUUID());
        req.setAmount(new BigDecimal("49.99"));
        req.setCurrency("USD");

        mockMvc.perform(post("/api/payments/intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    // -----------------------------------------------------------
    // 2) EXECUTE PAYMENT
    // -----------------------------------------------------------
    @Test
    void testExecutePayment() throws Exception {
        BillEntity bill = new BillEntity();
        bill.setCustomerId(UUID.randomUUID());
        bill.setName("Test Bill");
        bill.setAmount(new BigDecimal("20.00"));
        bill.setStatus(BillStatus.PENDING);
        bill.setDueDate(LocalDate.now().plusDays(1));
        UUID id = billRepository.save(bill).getId();

        // First create an intent
        PaymentIntentRequest req = new PaymentIntentRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setBillId(id);
        req.setMerchantId(UUID.randomUUID());
        req.setAmount(BigDecimal.valueOf(20.00));
        req.setCurrency("USD");

        String response = mockMvc.perform(post("/api/payments/intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String paymentId = om.readTree(response).get("paymentId").asText();

        ExecutePaymentRequest execReq = new ExecutePaymentRequest();
        execReq.setGatewayIdempotencyKey("idem-exec-001");

        mockMvc.perform(post("/api/payments/" + paymentId + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(execReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // -----------------------------------------------------------
    // 3) SCHEDULE PAYMENT
    // -----------------------------------------------------------
    @Test
    void testSchedulePayment() throws Exception {
        PaymentIntentRequest req = new PaymentIntentRequest();
        req.setCustomerId(customerId);
        req.setBillId(billId);
        req.setMerchantId(UUID.randomUUID());
        req.setAmount(BigDecimal.valueOf(15.50));
        req.setCurrency("USD");
        req.setScheduledDate(LocalDate.now().plusDays(3));

        mockMvc.perform(post("/api/payments/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.paymentId").exists());
    }

    // -----------------------------------------------------------
    // 4) CANCEL SCHEDULED PAYMENT
    // -----------------------------------------------------------
    @Test
    void testCancelScheduledPayment() throws Exception {
        // Create a scheduled payment first
        PaymentEntity scheduled = new PaymentEntity();
        scheduled.setCustomerId(customerId);
        scheduled.setBillId(billId);
        scheduled.setMerchantId(UUID.randomUUID());
        scheduled.setAmount(new BigDecimal("22.00"));
        scheduled.setCurrency("USD");
        scheduled.setStatus(com.wells.bill.assistant.entity.PaymentStatus.SCHEDULED);
        scheduled.setPaymentType(com.wells.bill.assistant.entity.PaymentType.SCHEDULED);
        scheduled.setScheduledDate(LocalDate.now().plusDays(5));
        scheduled.setPaymentId("pay_test_cancel");
        paymentRepository.save(scheduled);

        mockMvc.perform(post("/api/payments/pay_test_cancel/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cancelled"));

        PaymentEntity updated = paymentRepository.findByPaymentId("pay_test_cancel").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(com.wells.bill.assistant.entity.PaymentStatus.CANCELLED);
    }

    // -----------------------------------------------------------
    // 5) GET PAYMENT STATUS
    // -----------------------------------------------------------
    @Test
    void testGetPayment() throws Exception {
        PaymentEntity p = new PaymentEntity();
        p.setPaymentId("pay_lookup_001");
        p.setCustomerId(customerId);
        p.setBillId(billId);
        p.setMerchantId(UUID.randomUUID());
        p.setAmount(BigDecimal.valueOf(12.34));
        p.setCurrency("USD");
        p.setStatus(com.wells.bill.assistant.entity.PaymentStatus.CREATED);
        p.setPaymentType(com.wells.bill.assistant.entity.PaymentType.IMMEDIATE);
        paymentRepository.save(p);

        mockMvc.perform(get("/api/payments/pay_lookup_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("pay_lookup_001"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }
}
