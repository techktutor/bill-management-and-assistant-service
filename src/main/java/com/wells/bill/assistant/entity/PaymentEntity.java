package com.wells.bill.assistant.entity;

import com.wells.bill.assistant.model.ExecutedBy;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payments_payment_id", columnList = "payment_id", unique = true),
                @Index(name = "idx_payments_user_id", columnList = "user_id"),
                @Index(name = "idx_payments_bill_id", columnList = "bill_id"),
                @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_scheduled_date", columnList = "scheduled_date")
        })
@Getter
@Setter
@ToString(exclude = {"idempotencyKey", "gatewayReferenceId", "gatewayPayload"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "payment_id", nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "bill_id", nullable = false)
    private UUID billId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "executedBy", length = 20)
    private ExecutedBy executedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "execute_at")
    private Instant executedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "payment_reference", nullable = false, unique = true)
    private String paymentReference;

    @Column(name = "gateway_reference_id")
    private String gatewayReferenceId;

    @Column(name = "gateway_payload", columnDefinition = "jsonb")
    private String gatewayPayload;

    @Column(name = "failure_reason")
    private String failureReason;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;

        if (paymentType == null) {
            throw new IllegalStateException("paymentType must be set before persisting PaymentEntity");
        }

        if (status == null) {
            status = (paymentType == PaymentType.SCHEDULED)
                    ? PaymentStatus.SCHEDULED
                    : PaymentStatus.CREATED;
        }

        if (currency == null) {
            currency = "INR";
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
