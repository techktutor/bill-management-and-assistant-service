package com.wells.bill.assistant.util;

import java.util.UUID;

public final class ConversationContextHolder {

    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CONVERSATION_ID = new ThreadLocal<>();

    private ConversationContextHolder() {
    }

    public static void set(UUID userId, UUID conversationId) {
        USER_ID.set(userId);
        CONVERSATION_ID.set(conversationId);
    }

    public static UUID getUserId() {
        return USER_ID.get();
    }

    public static UUID getConversationId() {
        return CONVERSATION_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
        CONVERSATION_ID.remove();
    }
}

