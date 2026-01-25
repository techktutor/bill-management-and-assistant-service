package com.wells.bill.assistant.model;

public enum ReasonCode {

    // Generic
    NOT_FOUND,
    INFERRED,

    // Amount
    AMOUNT_LABEL_MATCHED,
    AMOUNT_NUMERIC_PARSED,

    // Date
    DATE_LABEL_MATCHED,
    DATE_FORMAT_PARSED,

    // Consumer
    STRONG_LABEL_MATCH,
    NUMERIC_PATTERN_MATCH,

    // Provider
    EXACT_KEYWORD_MATCH,

    DATE_RANGE_PARSED, // Category
    MONTH_YEAR_INFERRED, CATEGORY_KEYWORD_MATCHED
}

