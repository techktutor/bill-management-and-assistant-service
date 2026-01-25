package com.wells.bill.assistant.model;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record QueryBillsIntent(
        @NotBlank(message = "Message is required and cannot be blank")
        UUID userId
) implements Intent {
}
