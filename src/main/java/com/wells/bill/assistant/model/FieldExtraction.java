package com.wells.bill.assistant.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class FieldExtraction<T> {
    T value;
    int confidence;                 // 0–100
    List<ReasonCode> reasons;       // why we believe this
}

