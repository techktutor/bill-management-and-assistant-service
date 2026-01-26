package com.wells.bill.assistant.util;

public final class HighConfidencePrompt {

    public static String instructions(String conversationId) {
        return String.format("""
        Your current conversationId = %s.
        Never mix data across conversations.
        """, conversationId);
    }
}

