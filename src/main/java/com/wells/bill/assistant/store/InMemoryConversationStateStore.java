package com.wells.bill.assistant.store;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Component("inMemoryConversationStateStore")
public class InMemoryConversationStateStore implements ConversationStateStore {

    private static class Entry {
        final String value;
        final Instant expiresAt;

        Entry(String value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, Map<String, Entry>> store = new ConcurrentHashMap<>();

    @Override
    public String get(String conversationId, String key) {
        Map<String, Entry> convo = store.get(conversationId);
        if (convo == null) return null;

        Entry entry = convo.get(key);
        if (entry == null) return null;

        if (entry.expiresAt.isBefore(Instant.now())) {
            convo.remove(key); // auto-expire
            return null;
        }
        return entry.value;
    }

    @Override
    public void put(String conversationId, String key, String value, Duration ttl) {
        Instant expiresAt = Instant.now().plus(ttl);
        store.computeIfAbsent(conversationId, id -> new ConcurrentHashMap<>())
                .put(key, new Entry(value, expiresAt));
    }

    @Override
    public void clear(String conversationId, String key) {
        Map<String, Entry> convo = store.get(conversationId);
        if (convo != null) {
            convo.remove(key);
        }
    }
}
