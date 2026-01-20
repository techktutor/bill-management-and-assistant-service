package com.wells.bill.assistant.model;

public record RagAnswer(String answer, double confidence, boolean grounded, int chunksUsed) {
    public static RagAnswer blocked(String msg) {
        return new RagAnswer(msg, 0.0, false, 0);
    }

    public static RagAnswer warned(String msg, double confidence, int chunks) {
        return new RagAnswer(msg, confidence, true, chunks);
    }
}
