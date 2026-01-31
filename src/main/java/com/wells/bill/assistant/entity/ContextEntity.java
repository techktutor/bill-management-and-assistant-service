package com.wells.bill.assistant.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_context")
public class ContextEntity {

    @Id
    private String contextKey;

    private UUID userId;
    private UUID conversationId;
    private long lastAccessTime;

    public ContextEntity() {
    }

    public ContextEntity(String contextKey, UUID userId, UUID conversationId) {
        this.contextKey = contextKey;
        this.userId = userId;
        this.conversationId = conversationId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public void touch() {
        this.lastAccessTime = System.currentTimeMillis();
    }

}

