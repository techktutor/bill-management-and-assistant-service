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

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "gateway_reference", length = 200)
    private String gatewayReference;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Lob
    @Column(name = "gateway_payload", columnDefinition = "text")
    private String gatewayPayload;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.paymentId == null) this.paymentId = "pay_" + UUID.randomUUID();
        if (this.status == null) {
            this.status = (this.paymentType == PaymentType.SCHEDULED)
                    ? PaymentStatus.SCHEDULED
                    : PaymentStatus.CREATED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
