package com.wells.bill.assistant.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class CaptureResponse {
    private UUID paymentId;
    private PaymentStatus status;

    public CaptureResponse(UUID paymentId, PaymentStatus status) {
        this.paymentId = paymentId;
        this.status = status;
    }
}
