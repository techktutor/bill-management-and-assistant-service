package com.wells.bill.assistant.model;

public enum DataQualityDecision {

    /** Safe to auto-process / auto-pay */
    HIGH_CONFIDENCE,

    /** Acceptable but user should confirm some fields */
    NEEDS_CONFIRMATION,

    /** Block processing – data is unreliable */
    NOT_CONFIDENT,

    UNKNOWN
}
