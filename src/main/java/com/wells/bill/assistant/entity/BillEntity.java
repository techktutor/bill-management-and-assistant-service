package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Production-ready BillEntity with audit, RAG metadata fields, and monetary safety.
 */
@Entity
@Table(name = "bills", indexes = {
        @Index(name = "idx_bills_customer_id", columnList = "customer_id"),
        @Index(name = "idx_bills_status", columnList = "status"),
        @Index(name = "idx_bills_vendor", columnList = "vendor"),
        @Index(name = "idx_bills_due_date", columnList = "due_date"),
        @Index(name = "idx_bills_autopay_date", columnList = "auto_pay_scheduled_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @DecimalMin("0.01")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BillStatus status;

    @Column(length = 200)
    private String vendor;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BillCategory category;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "late_fee", precision = 14, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "payment_id")
    private String paymentId;

    /**
     * Last successful payment reference (e.g., pay_...)
     */
    @Column(name = "last_successful_payment_id", length = 100)
    private String lastSuccessfulPaymentId;

    /**
     * Original document reference (S3/GCS URL or internal path)
     */
    @Column(name = "document_url", length = 1000)
    private String documentUrl;

    /**
     * Extracted OCR/text used for RAG. Use @Lob for large text.
     * Stored as plain text (not JSON) unless you change to structured JSON type.
     */
    @Lob
    @Column(name = "extracted_text")
    private String extractedText;

    @Column(length = 50)
    private String source;

    // Auto-pay fields
    @Column(name = "auto_pay_enabled", nullable = false)
    private Boolean autoPayEnabled = Boolean.FALSE;

    @Column(name = "auto_pay_scheduled_date")
    private LocalDate autoPayScheduledDate;

    @Column(name = "auto_pay_payment_id", length = 100)
    private String autoPayPaymentId;

    @Column(name = "last_auto_pay_run")
    private Instant lastAutoPayRun;

    // Optional soft-delete flag
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    // Optional external reference / invoice number
    @Column(name = "reference_number", length = 128)
    private String referenceNumber;

    // Short human-readable description
    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = BillStatus.PENDING;
        normalizeFields();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        normalizeFields();
        validatePeriod();
    }

    private void normalizeFields() {
        if (this.vendor != null) this.vendor = this.vendor.trim();
        if (this.name != null) this.name = this.name.trim();
        if (this.referenceNumber != null) this.referenceNumber = this.referenceNumber.trim();
    }

    private void validatePeriod() {
        if (this.periodStart != null && this.periodEnd != null && this.periodStart.isAfter(this.periodEnd)) {
            throw new IllegalStateException("Billing period start cannot be after end");
        }
    }
}
