package com.wells.bill.assistant.model;

import java.time.Duration;
import java.time.Instant;

public class ExpiringConversation {

    private final ConversationContext context;
    private final Instant expiresAt;

    public ExpiringConversation(ConversationContext context, Duration ttl) {
        this.context = context;
        this.expiresAt = Instant.now().plus(ttl);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public ConversationContext context() {
        return context;
    }
}
