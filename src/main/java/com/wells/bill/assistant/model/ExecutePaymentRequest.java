package com.wells.bill.assistant.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ExecutePaymentRequest {

    @NotBlank
    private String executedBy;

    private String gatewayIdempotencyKey;
    /**
     * Payment intent ID to execute (e.g., pay_xxx).
     */
    private String paymentId;

    /**
     * The customer initiating the execution.
     */
    private UUID customerId;

    /**
     * Tokenized card reference such as tok_xxx or card_xxx.
     */
    private String cardToken;
}
