package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "card_tokens")
public class CardToken {
    @Id
    private UUID cardId;

    @Column(unique = true)
    private String token;

    private String last4;
    private String brand;
    private Integer expMonth;
    private Integer expYear;
    private String fingerprint;

    @Lob
    private byte[] encryptedPayload;
    private Instant createdAt;
}
