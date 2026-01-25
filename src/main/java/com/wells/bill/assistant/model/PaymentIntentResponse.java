package com.wells.bill.assistant.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Setter
@Getter
public class PaymentIntentResponse {

    private UUID paymentId;

    private String idempotencyKey;

    private PaymentStatus status;

    private PaymentType paymentType;

    private BigDecimal amount;

    private String currency;

    private UUID customerId;

    private UUID billId;

    private LocalDate scheduledDate;

    private Instant createdAt;

    private Instant updatedAt;
}