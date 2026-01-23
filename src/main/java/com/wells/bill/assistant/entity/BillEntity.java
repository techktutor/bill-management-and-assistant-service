package com.wells.bill.assistant.entity;

import com.wells.bill.assistant.model.DataQualityDecision;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bills",
        indexes = {
                @Index(name = "idx_bill_user", columnList = "user_id"),
                @Index(name = "idx_bill_consumer", columnList = "consumer_id"),
                @Index(name = "idx_bill_consumer_name", columnList = "consumer_name"),
                @Index(name = "idx_bill_category", columnList = "category"),
                @Index(name = "idx_bill_due_date", columnList = "due_date"),
                @Index(name = "idx_bill_status", columnList = "status"),
                @Index(name = "idx_bill_payment", columnList = "payment_id")
        }
)
@Getter
@Setter
@ToString(exclude = "metadata")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BillEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consumer_id", nullable = false)
    private String consumerNumber;

    @Column(name = "consumer_name")
    private String consumerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private BillCategory billCategory;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Column(name = "service_number")
    private String serviceNumber;

    @Column(name = "billing_start_date")
    private LocalDate billingStartDate;

    @Column(name = "billing_end_date")
    private LocalDate billingEndDate;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BillStatus status;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "ingested_at")
    private Instant ingestedAt;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private Integer confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_decision", length = 32)
    private DataQualityDecision confidenceDecision;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;

        if (this.status == null) {
            this.status = BillStatus.UPLOADED;
        }

        if (this.currency == null) {
            this.currency = "INR";
        }

        if (this.chunkCount == null) {
            this.chunkCount = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
