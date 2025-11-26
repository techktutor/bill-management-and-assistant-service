package com.wells.bill.assistant.entity;

import com.wells.bill.assistant.model.PaymentScheduleStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "scheduled_payments")
public class ScheduledPaymentEntity {
    @Id
    private UUID id;

    private String billId;

    private UUID paymentId;

    private Long amount;

    private String currency;

    private LocalDate scheduledDate;

    private String vendor;

    private String category;

    @Lob
    private String originalBillText;

    @Enumerated(EnumType.STRING)
    private PaymentScheduleStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID merchantId;

    private UUID customerId;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}