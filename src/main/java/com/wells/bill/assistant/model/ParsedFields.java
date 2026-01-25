package com.wells.bill.assistant.model;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ParsedFields(
        FieldExtraction<Money> amountDue,
        FieldExtraction<LocalDate> dueDate,
        FieldExtraction<DateRange> billingPeriod,
        FieldExtraction<String> consumerName,
        FieldExtraction<String> consumerNumber,
        FieldExtraction<String> providerName,
        FieldExtraction<String> billCategory
) {
}
