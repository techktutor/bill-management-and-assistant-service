package com.wells.bill.assistant.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class CreatePaymentResponse {
    private UUID paymentId;
    private String paymentIntentId;
    private PaymentStatus status;

    public CreatePaymentResponse(UUID paymentId, String paymentIntentId, PaymentStatus status) {
        this.paymentId = paymentId;
        this.paymentIntentId = paymentIntentId;
        this.status = status;
    }
}
