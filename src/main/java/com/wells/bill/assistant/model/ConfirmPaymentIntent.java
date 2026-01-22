package com.wells.bill.assistant.model;

import java.util.UUID;

public record ConfirmPaymentIntent(UUID conversationId) implements Intent {}
