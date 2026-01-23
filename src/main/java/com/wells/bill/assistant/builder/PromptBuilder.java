package com.wells.bill.assistant.builder;

import com.wells.bill.assistant.model.DataQualityDecision;
import com.wells.bill.assistant.util.BaseSystemPrompt;
import com.wells.bill.assistant.util.HighConfidencePrompt;
import com.wells.bill.assistant.util.LowConfidencePrompt;
import com.wells.bill.assistant.util.MediumConfidencePrompt;

public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String buildSystemPrompt(
            String userId,
            DataQualityDecision decision
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(BaseSystemPrompt.base(userId)).append("\n");

        switch (decision) {
            case HIGH_CONFIDENCE -> prompt.append(HighConfidencePrompt.instructions());

            case NEEDS_CONFIRMATION -> prompt.append(MediumConfidencePrompt.instructions());

            case NOT_CONFIDENT -> prompt.append(LowConfidencePrompt.instructions());
        }

        return prompt.toString();
    }
}
