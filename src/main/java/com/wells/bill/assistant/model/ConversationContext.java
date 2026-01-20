package com.wells.bill.assistant.model;

import java.time.Instant;

public record ConversationContext(
        String conversationId,
        String userId,
        ConversationState state,
        PendingPayment pendingPayment,
        Instant confirmationRequestedAt
) {

    public static ConversationContext newConversation(
            String conversationId,
            String userId
    ) {
        return new ConversationContext(
                conversationId,
                userId,
                ConversationState.IDLE,
                null,
                null
        );
    }

    public ConversationContext apply(GuardResult guardResult) {

        if (guardResult.nextState().isEmpty()) {
            return this;
        }

        ConversationState nextState = guardResult.nextState().get();

        Instant confirmationAt =
                nextState == ConversationState.AWAITING_CONFIRMATION
                        ? Instant.now()
                        : confirmationRequestedAt;

        return new ConversationContext(
                conversationId,
                userId,
                nextState,
                guardResult.pendingPayment().orElse(pendingPayment),
                confirmationAt
        );
    }

    public ConversationContext executingPayment() {

        if (state != ConversationState.AWAITING_CONFIRMATION
                && state != ConversationState.PAYMENT_INTENT_ALLOWED) {
            throw new IllegalStateException(
                    "Cannot move to EXECUTING_PAYMENT from " + state
            );
        }

        return new ConversationContext(
                conversationId,
                userId,
                ConversationState.EXECUTING_PAYMENT,
                pendingPayment,
                confirmationRequestedAt
        );
    }

    public ConversationContext completed() {
        return new ConversationContext(
                conversationId,
                userId,
                ConversationState.COMPLETED,
                null,
                null
        );
    }

    public boolean isConfirmationExpired(long ttlSeconds) {

        if (confirmationRequestedAt == null) {
            return true;
        }

        return Instant.now().isAfter(
                confirmationRequestedAt.plusSeconds(ttlSeconds)
        );
    }
}
