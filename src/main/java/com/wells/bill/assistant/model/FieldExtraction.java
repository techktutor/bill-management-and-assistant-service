package com.wells.bill.assistant.model;

import lombok.Builder;

import java.util.List;

/**
 * @param confidence 0–100
 * @param reasons    why we believe this
 */
@Builder
public record FieldExtraction<T>(
        T value, int confidence,
        List<ReasonCode> reasons
) {
}
