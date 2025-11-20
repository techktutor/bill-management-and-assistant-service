package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;


    private UUID aggregateId;
    private String eventType;


    @Column(columnDefinition = "jsonb")
    private String payload;


    private boolean processed;
    private Instant createdAt;
}
