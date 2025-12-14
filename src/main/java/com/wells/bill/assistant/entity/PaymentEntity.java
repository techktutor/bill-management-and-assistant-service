package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_payment_id", columnList = "payment_id", unique = true),
        @Index(name = "idx_payments_customer_id", columnList = "customer_id"),
        @Index(name = "idx_payments_bill_id", columnList = "bill_id"),
        @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_payments_status_scheduled_date", columnList = "status, scheduled_date")
})
@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 100)
    private String paymentId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "bill_id", nullable = false)
    private UUID billId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType; // INSTANT / SCHEDULED

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    // ===== AI / Approval Guardrails =====
    @Column(name = "approval_source", length = 20, nullable = false)
    private String approvalSource; // USER, AI_SUGGESTED, SYSTEM

    @Column(name = "approved_at")
    private Instant approvedAt;

    // ===== Gateway =====
    private String gatewayReference;

    @Lob
    private String gatewayPayload;

    private String failureReason;

    private Instant executedAt;
    private Instant cancelledAt;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (paymentId == null) paymentId = "pay_" + UUID.randomUUID();
        if (status == null) {
            status = (paymentType == PaymentType.SCHEDULED)
                    ? PaymentStatus.SCHEDULED
                    : PaymentStatus.CREATED;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
