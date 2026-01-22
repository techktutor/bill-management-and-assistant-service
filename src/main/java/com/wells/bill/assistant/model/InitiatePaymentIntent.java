package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentIntent(UUID userId, String billId, BigDecimal amount) implements Intent {
}
