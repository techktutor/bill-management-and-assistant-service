package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.PaymentStatus;
import com.wells.bill.assistant.entity.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Setter
@Getter
public class PaymentIntentResponse {
    /**
     * Public payment intent identifier (pay_xxx).
     */
    private String paymentId;

    /**
     * Idempotency key used when creating this intent.
     */
    private String idempotencyKey;

    /**
     * CREATED or SCHEDULED (intent is never SUCCESS).
     */
    private PaymentStatus status;

    private PaymentType paymentType;

    private BigDecimal amount;

    private String currency;

    private UUID customerId;

    private Long billId;

    /**
     * Applies only when paymentType == SCHEDULED.
     */
    private LocalDate scheduledDate;

    private Instant createdAt;

    private Instant updatedAt;
}