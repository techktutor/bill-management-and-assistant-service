package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private UUID id;

    private UUID merchantId;
    private UUID customerId;

    private Long amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String paymentIntentId;
    @Version
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;
}

