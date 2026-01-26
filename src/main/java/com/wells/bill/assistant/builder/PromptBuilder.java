package com.wells.bill.assistant.builder;

import com.wells.bill.assistant.util.BaseSystemPrompt;
import com.wells.bill.assistant.util.HighConfidencePrompt;

public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String buildSystemPrompt(
            String userId,
            String conversationId
    ) {
        return BaseSystemPrompt.base(userId) + "\n" +
                HighConfidencePrompt.instructions(conversationId);
    }
}
