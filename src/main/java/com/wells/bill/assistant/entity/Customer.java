package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private UUID id;
    private UUID merchantId;
    private String name;
    private String email;
    private Instant createdAt;
}
