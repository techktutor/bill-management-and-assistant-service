package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_user_id", columnList = "user_id"),
                @Index(name = "idx_customers_external_id", columnList = "external_customer_id"),
                @Index(name = "idx_customers_email", columnList = "email"),
                @Index(name = "idx_customers_phone", columnList = "phone_country_code, phone_number"),
                @Index(name = "idx_customers_status", columnList = "status")
        }
)
@Getter
@Setter
@ToString(exclude = "metadata")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CustomerEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /* =======================
       Identity
       ======================= */

    @Column(name = "external_customer_id", length = 64, unique = true)
    private String externalCustomerId;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    /* =======================
       Customer Type
       ======================= */

    @Column(name = "customer_type", nullable = false, length = 20)
    private String customerType; // INDIVIDUAL | BUSINESS

    /* =======================
       Personal / Business Info
       ======================= */

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "company_name", length = 255)
    private String companyName;

    /* =======================
       Contact Info
       ======================= */

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_country_code", length = 5)
    private String phoneCountryCode;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /* =======================
       Address
       ======================= */

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 2)
    private String country; // ISO-3166-1 alpha-2

    /* =======================
       Status & Compliance
       ======================= */

    @Column(name = "status", nullable = false, length = 32)
    private String status; // ACTIVE | INACTIVE | SUSPENDED | CLOSED

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "kyc_status", length = 32)
    private String kycStatus; // PENDING | VERIFIED | FAILED

    @Column(name = "risk_score")
    private Integer riskScore;

    /* =======================
       Metadata
       ======================= */

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /* =======================
       Optimistic Locking
       ======================= */

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /* =======================
       Auditing
       ======================= */

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /* =======================
       Lifecycle Hooks
       ======================= */

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
