package com.wells.bill.assistant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "user_context",
        indexes = @Index(name = "idx_last_access", columnList = "lastAccessTime")
)
@NoArgsConstructor
public class ContextEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID contextId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID conversationId;

    @Column(nullable = false)
    private long lastAccessTime;

    @Version
    private long version;

    public ContextEntity(UUID contextId, UUID userId, UUID conversationId, long now) {
        this.contextId = contextId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.lastAccessTime = now;
    }

    public void touch(long now) {
        this.lastAccessTime = now;
    }

    public void resetConversation(long now) {
        this.conversationId = UUID.randomUUID();
        this.lastAccessTime = now;
    }
}
