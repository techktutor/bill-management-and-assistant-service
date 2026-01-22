package com.wells.bill.assistant.entity;

public enum BillStatus {
    UPLOADED,   // Bill received (file/API)
    INGESTED,   // OCR + text extraction completed
    VERIFIED,   // Data validated / user confirmed
    PAID,       // Payment successful
    OVERDUE,    // Due date passed without payment
    FAILED,     // Ingestion / verification / payment failed
    CANCELLED   // Bill invalidated by user/system
}
