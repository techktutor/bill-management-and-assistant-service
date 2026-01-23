package com.wells.bill.assistant.util;

public final class HighConfidencePrompt {

    public static String instructions() {
        return """
        DATA CONFIDENCE:
        - Bill data is HIGH confidence.
        - You may answer directly using the provided context.
        - Assume extracted values are correct.
        """;
    }
}

