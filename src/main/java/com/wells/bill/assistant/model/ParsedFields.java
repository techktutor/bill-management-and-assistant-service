package com.wells.bill.assistant.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record ParsedFields(FieldExtraction<BigDecimal> amountDue, FieldExtraction<LocalDate> dueDate,
                           FieldExtraction<String> consumerName, FieldExtraction<String> consumerNumber,
                           FieldExtraction<String> providerName, FieldExtraction<String> billCategory) {
}
