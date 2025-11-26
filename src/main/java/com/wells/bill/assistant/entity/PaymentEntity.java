package com.wells.bill.assistant.entity;

import com.wells.bill.assistant.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    private UUID id;

    private UUID merchantId;

    private UUID customerId;

    private Long amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String paymentId;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.paymentId == null) this.paymentId = "pay_" + UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}