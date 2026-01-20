package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.RagAnswer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RagAnswerCacheInMemory implements RagAnswerCache {

    /**
     * conversationId → (cacheKey → entry)
     */
    private final Map<String, Map<String, CacheEntry>> store = new ConcurrentHashMap<>();

    @Override
    public Optional<RagAnswer> get(String conversationId, String billId, String normalizedQuestion) {
        if (conversationId == null || billId == null || normalizedQuestion == null) {
            return Optional.empty();
        }

        Map<String, CacheEntry> conversationCache = store.get(conversationId);
        if (conversationCache == null) {
            return Optional.empty();
        }

        String key = key(billId, normalizedQuestion);
        CacheEntry entry = conversationCache.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired()) {
            conversationCache.remove(key);
            return Optional.empty();
        }

        return Optional.of(entry.answer());
    }

    @Override
    public void put(String conversationId, String billId, String normalizedQuestion, RagAnswer answer, Duration ttl) {
        if (conversationId == null || billId == null || normalizedQuestion == null || answer == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }

        store.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
                .put(key(billId, normalizedQuestion), new CacheEntry(answer, Instant.now().plus(ttl)));
    }

    @Override
    public void clearConversation(String conversationId) {
        if (conversationId == null) {
            return;
        }
        store.remove(conversationId);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------
    private String key(String billId, String normalizedQuestion) {
        return billId + "::" + normalizedQuestion;
    }

    private record CacheEntry(RagAnswer answer, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
