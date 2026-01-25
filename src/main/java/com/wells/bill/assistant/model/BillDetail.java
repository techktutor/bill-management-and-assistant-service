package com.wells.bill.assistant.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillDetail(
        UUID id,
        UUID userId,

        // Consumer / Provider
        String consumerNumber,
        String consumerName,
        String providerName,
        String serviceNumber,

        // Billing
        BillCategory billCategory,
        LocalDate dueDate,
        DateRange billingPeriod,

        // Amount
        Money amountDue,

        // Status & Payment
        BillStatus status,
        UUID paymentId,

        // AI / Ingestion
        Instant ingestedAt,
        Integer chunkCount,

        // Audit
        Instant createdAt,
        Instant updatedAt,

        // Confidence Score
        Integer confidenceScore,
        DataQualityDecision confidenceDecision
) {

    /* ---------- Defaults & validation ---------- */
    public BillDetail {
        if (status == null) status = BillStatus.UPLOADED;
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (ingestedAt == null) ingestedAt = Instant.now();
    }

    /* ---------- Builder entry point ---------- */
    public static Builder builder() {
        return new Builder();
    }

    /* ---------- Builder ---------- */
    public static final class Builder {
        private UUID id;
        private UUID userId;

        private String consumerNumber;
        private String consumerName;
        private String providerName;
        private String serviceNumber;

        private BillCategory billCategory;
        private LocalDate dueDate;
        private DateRange billingPeriod;

        private Money amountDue;

        private BillStatus status = BillStatus.UPLOADED;
        private UUID paymentId;

        private final Instant ingestedAt = Instant.now();
        private Integer chunkCount;

        private final Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private Integer confidenceScore;
        private DataQualityDecision confidenceDecision;

        private Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder consumerNumber(String consumerNumber) {
            this.consumerNumber = consumerNumber;
            return this;
        }

        public Builder consumerName(String consumerName) {
            this.consumerName = consumerName;
            return this;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder serviceNumber(String serviceNumber) {
            this.serviceNumber = serviceNumber;
            return this;
        }

        public Builder billCategory(BillCategory billCategory) {
            this.billCategory = billCategory;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder billingPeriod(DateRange billingPeriod) {
            this.billingPeriod = billingPeriod;
            return this;
        }

        public Builder amountDue(Money amountDue) {
            this.amountDue = amountDue;
            return this;
        }

        public Builder status(BillStatus status) {
            this.status = status;
            return this;
        }

        public Builder paymentId(UUID paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder chunkCount(Integer chunkCount) {
            this.chunkCount = chunkCount;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder confidenceScore(Integer confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Builder confidenceDecision(DataQualityDecision confidenceDecision) {
            this.confidenceDecision = confidenceDecision;
            return this;
        }

        public BillDetail build() {
            return new BillDetail(
                    id,
                    userId,
                    consumerNumber,
                    consumerName,
                    providerName,
                    serviceNumber,
                    billCategory,
                    dueDate,
                    billingPeriod,
                    amountDue,
                    status,
                    paymentId,
                    ingestedAt,
                    chunkCount,
                    createdAt,
                    updatedAt,
                    confidenceScore,
                    confidenceDecision
            );
        }
    }
}
