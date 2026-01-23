package com.wells.bill.assistant.model;

import lombok.Builder;

import java.util.List;

/**
 * @param reasons human-readable explanations
 */
@Builder
public record ConfidenceValidationResult(DataQualityDecision decision, List<String> reasons) {
}

