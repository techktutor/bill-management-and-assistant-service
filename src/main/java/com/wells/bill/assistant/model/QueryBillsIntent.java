package com.wells.bill.assistant.model;

public record QueryBillsIntent(
        java.util.@jakarta.validation.constraints.NotBlank(message = "Message is required and cannot be blank") UUID userId) implements Intent {}
