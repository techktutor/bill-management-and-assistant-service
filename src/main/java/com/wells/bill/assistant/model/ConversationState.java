package com.wells.bill.assistant.model;

public enum ConversationState {
    IDLE,
    AWAITING_CONFIRMATION,
    PAYMENT_INTENT_ALLOWED,
    EXECUTING_PAYMENT,
    COMPLETED,
    FAILED
}
