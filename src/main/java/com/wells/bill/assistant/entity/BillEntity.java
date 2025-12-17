package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bills", indexes = {
        @Index(name = "idx_bills_customer_id", columnList = "customer_id"),
        @Index(name = "idx_bills_status", columnList = "status"),
        @Index(name = "idx_bills_vendor", columnList = "vendor"),
        @Index(name = "idx_bills_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillEntity {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 200)
    private String consumerName;

    @Column(nullable = false, length = 200)
    private String consumerNumber;

    @Column(nullable = false, length = 200)
    private String fileName;

    @Column(length = 200)
    private String vendor;

    @Enumerated(EnumType.STRING)
    private BillCategory category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillStatus status;

    // ===== Ingestion / AI =====
    @Column(name = "ingested_at")
    private Instant ingestedAt;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    // ===== Auto-pay rule (NOT execution) =====
    @Column(name = "auto_pay_enabled", nullable = false)
    private Boolean autoPayEnabled = Boolean.FALSE;

    @Column(name = "auto_pay_day_of_month")
    private Integer autoPayDayOfMonth;

    // ===== Audit =====
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "last_successful_payment_id", length = 100)
    private String lastSuccessfulPaymentId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = BillStatus.UPLOADED;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
