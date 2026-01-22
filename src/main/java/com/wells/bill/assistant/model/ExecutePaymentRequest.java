package com.wells.bill.assistant.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ExecutePaymentRequest {

    @NotBlank
    private ExecutedBy executedBy;

    /**
     * Payment intent ID to execute (e.g., pay_xxx).
     */
    private UUID paymentId;

    /**
     * The customer initiating the execution.
     */
    private UUID userId;
}
