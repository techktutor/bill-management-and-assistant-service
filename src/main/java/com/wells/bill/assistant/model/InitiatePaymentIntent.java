package com.wells.bill.assistant.model;

import java.math.BigDecimal;

public record InitiatePaymentIntent(String userId, String billId, BigDecimal amount) implements Intent {
}
