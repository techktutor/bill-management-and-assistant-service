package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillDetail(
        UUID id,
        UUID userId,

        // Consumer / Provider
        String consumerId,
        String consumerName,
        String providerName,
        String serviceNumber,

        // Billing
        BillCategory billCategory,
        LocalDate billingStartDate,
        LocalDate billingEndDate,
        LocalDate dueDate,

        // Amount
        BigDecimal amountDue,
        String currency,

        // Status & Payment
        BillStatus status,
        UUID paymentId,

        // AI / Ingestion
        Instant ingestedAt,
        Integer chunkCount,

        // Audit
        Instant createdAt,
        Instant updatedAt
) {

    /* ---------- Defaults & validation ---------- */
    public BillDetail {
        if (currency == null) currency = "INR";
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

        private String consumerId;
        private String consumerName;
        private String providerName;
        private String serviceNumber;

        private BillCategory billCategory;
        private LocalDate billingStartDate;
        private LocalDate billingEndDate;
        private LocalDate dueDate;

        private BigDecimal amountDue;
        private String currency = "INR";

        private BillStatus status = BillStatus.UPLOADED;
        private UUID paymentId;

        private final Instant ingestedAt = Instant.now();
        private Integer chunkCount;

        private final Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

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

        public Builder consumerId(String consumerId) {
            this.consumerId = consumerId;
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

        public Builder billingStartDate(LocalDate billingStartDate) {
            this.billingStartDate = billingStartDate;
            return this;
        }

        public Builder billingEndDate(LocalDate billingEndDate) {
            this.billingEndDate = billingEndDate;
            return this;
        }

        public Builder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder amountDue(BigDecimal amountDue) {
            this.amountDue = amountDue;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
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

        public BillDetail build() {
            return new BillDetail(
                    id,
                    userId,
                    consumerId,
                    consumerName,
                    providerName,
                    serviceNumber,
                    billCategory,
                    billingStartDate,
                    billingEndDate,
                    dueDate,
                    amountDue,
                    currency,
                    status,
                    paymentId,
                    ingestedAt,
                    chunkCount,
                    createdAt,
                    updatedAt
            );
        }
    }
}
