package com.wells.bill.assistant.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingPayment(String billId, BigDecimal amount, LocalDate scheduledDate) {
}
