package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PaymentIntentRequest {

    private UUID customerId;

    private Long billId;

    private UUID merchantId;

    private BigDecimal amount;

    private String currency = "USD";

    private String idempotencyKey;

    private LocalDate scheduledDate;

    private PaymentType paymentType;
}