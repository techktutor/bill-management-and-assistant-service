package com.wells.bill.assistant.model;

import lombok.Getter;

import java.util.Optional;

public final class GuardResult {

    @Getter
    private final boolean allowed;
    private final String userMessage;
    private final ConversationState nextState;
    private final PendingPayment pendingPayment;

    private GuardResult(
            boolean allowed,
            String userMessage,
            ConversationState nextState,
            PendingPayment pendingPayment
    ) {
        this.allowed = allowed;
        this.userMessage = userMessage;
        this.nextState = nextState;
        this.pendingPayment = pendingPayment;
    }

    // -----------------------------
    // Static factories
    // -----------------------------

    public static GuardResult allow() {
        return new GuardResult(true, null, null, null);
    }

    public static GuardResult requestConfirmation(
            String message,
            PendingPayment pendingPayment,
            ConversationState nextState
    ) {
        return new GuardResult(false, message, nextState, pendingPayment);
    }

    public static GuardResult block(String message) {
        return new GuardResult(false, message, null, null);
    }

    public static GuardResult allowAndTransition(
            ConversationState nextState
    ) {
        return new GuardResult(true, null, nextState, null);
    }

    public String userMessage() {
        return userMessage;
    }

    public Optional<ConversationState> nextState() {
        return Optional.ofNullable(nextState);
    }

    public Optional<PendingPayment> pendingPayment() {
        return Optional.ofNullable(pendingPayment);
    }
}
