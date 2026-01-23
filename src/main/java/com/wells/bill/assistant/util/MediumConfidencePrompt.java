package com.wells.bill.assistant.util;

public final class MediumConfidencePrompt {

    public static String instructions() {
        return """
        DATA CONFIDENCE:
        - Bill data is MEDIUM confidence.
        - Some fields may be inaccurate or incomplete.
        - Answer conservatively.
        - If a value seems missing or uncertain, ask the user to confirm it.
        - Clearly indicate uncertainty where applicable.
        """;
    }
}

