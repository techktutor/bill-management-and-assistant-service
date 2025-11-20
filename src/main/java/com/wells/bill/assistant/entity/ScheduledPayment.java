package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "scheduled_payments")
public class ScheduledPayment {
    @Id
    private UUID id;


    private String billId;


    private UUID paymentId; // payment record created at authorize time


    private Long amount; // in cents


    private String currency;


    private LocalDate scheduledDate;


    @Enumerated(EnumType.STRING)
    private PaymentScheduleStatus status;


    private Instant createdAt;
    private Instant updatedAt;
}
