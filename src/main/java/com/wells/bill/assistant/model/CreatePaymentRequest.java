package com.wells.bill.assistant.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePaymentRequest {
    @NotNull
    private UUID merchantId;

    @NotNull
    private UUID customerId;

    @NotNull
    @Min(1)
    private Long amount;

    @NotBlank
    private String currency = "usd";
}
