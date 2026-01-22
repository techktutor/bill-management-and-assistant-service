package com.wells.bill.assistant.model;

import java.util.UUID;

public final class Context {

    private final UUID conversationId;
    private final UUID userId;
    private volatile long lastAccessTime;

    public Context(UUID conversationId, UUID userId) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public void touch() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public long lastAccessTime() {
        return lastAccessTime;
    }

    public UUID conversationId() {
        return conversationId;
    }

    public UUID userId() {
        return userId;
    }
}
