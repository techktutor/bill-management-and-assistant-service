package com.wells.bill.assistant.model;

public enum DataQualityDecision {
    HIGH_CONFIDENCE,
    MEDIUM_CONFIDENCE,
    LOW_CONFIDENCE;

    public static DataQualityDecision fromScore(int score) {
        if (score >= 90) return HIGH_CONFIDENCE;
        if (score <= 70) return LOW_CONFIDENCE;
        return MEDIUM_CONFIDENCE;
    }
}
