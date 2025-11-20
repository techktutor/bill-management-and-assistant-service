package com.wells.bill.assistant.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey {
    @Id
    private String key;

    @Lob
    private String responseSnapshot;

    private Instant createdAt;
    private Instant expiresAt;

    public IdempotencyKey(String key, String responseSnapshot, Instant createdAt, Instant expiresAt) {
        this.key = key;
        this.responseSnapshot = responseSnapshot;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public IdempotencyKey() {

    }
}
