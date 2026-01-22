package com.wells.bill.assistant.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class PaymentIntentRequest {
    @NotNull(message = "userId is required")
    private UUID userId;
    @NotNull(message = "billId is required")
    private UUID billId;
    @NotNull(message = "amount is required")
    private BigDecimal amount;
    @NotNull(message = "currency is required")
    private String currency;
    @NotNull(message = "idempotencyKey is required")
    private String idempotencyKey;
    private LocalDate scheduledDate;
    private ExecutedBy executedBy;
}