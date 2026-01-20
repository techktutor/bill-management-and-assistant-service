package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SchedulePaymentIntent(String userId, String billId, LocalDate scheduledDate, BigDecimal amount) implements Intent {
}
