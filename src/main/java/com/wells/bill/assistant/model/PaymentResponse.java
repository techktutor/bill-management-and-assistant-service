package com.wells.bill.assistant.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PaymentResponse {

    private UUID paymentId;

    private PaymentStatus status;

    private PaymentType paymentType;

    private BigDecimal amount;

    private String currency;

    private UUID customerId;

    private UUID billId;

    private LocalDate scheduledDate;

    private Instant executedAt;

    private Instant cancelledAt;

    private String gatewayReference;

    private String failureReason;

    private Instant createdAt;

    private Instant updatedAt;
}
