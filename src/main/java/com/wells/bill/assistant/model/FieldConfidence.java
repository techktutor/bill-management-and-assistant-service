package com.wells.bill.assistant.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FieldConfidence {
    int amountDue;
    int dueDate;
    int consumerName;
    int consumerNumber;
    int providerName;
    int billCategory;
}
