package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.ConversationContext;
import com.wells.bill.assistant.model.ExpiringConversation;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Component("conversationStateStoreInMemory")
public class ConversationStateStoreInMemory implements ConversationStateStore {

    private final Map<String, ExpiringConversation> store = new ConcurrentHashMap<>();

    @Override
    public ConversationContext load(String conversationId, String userId) {
        ExpiringConversation existing = store.get(conversationId);

        if (existing != null) {
            if (!existing.isExpired()) {
                return existing.context();
            }
            store.remove(conversationId);
        }

        ConversationContext fresh = ConversationContext.newConversation(conversationId, userId);

        // Default TTL for new conversations
        save(fresh, Duration.ofMinutes(10));

        return fresh;
    }

    @Override
    public void save(ConversationContext context, Duration ttl) {
        store.put(context.conversationId(), new ExpiringConversation(context, ttl));
    }

    @Override
    public void delete(String conversationId) {
        store.remove(conversationId);
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanupExpired() {
        store.entrySet().removeIf(
                entry -> entry.getValue().isExpired()
        );
    }
}
