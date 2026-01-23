package com.wells.bill.assistant.util;

public final class LowConfidencePrompt {

    public static String instructions() {
        return """
        DATA CONFIDENCE:
        - Bill data is LOW confidence.
        - You MUST NOT answer factual questions about amounts, dates, or providers.
        - Ask the user to re-upload the bill or confirm details manually.
        """;
    }
}

