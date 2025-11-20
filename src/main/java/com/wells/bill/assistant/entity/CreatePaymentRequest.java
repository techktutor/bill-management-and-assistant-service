package com.wells.bill.assistant.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class CreatePaymentRequest {
    private UUID merchantId;
    private UUID customerId;
    private Long amount;
    private String currency;
    private String cardToken;
}
