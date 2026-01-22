package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.Context;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContextStoreInMemory {

    private static final long EXPIRY_MS = 10 * 60 * 1000; // 10 minutes

    private final ConcurrentHashMap<String, Context> store = new ConcurrentHashMap<>();

    public Context get(String key) {
        long now = System.currentTimeMillis();

        return store.compute(key, (k, existing) -> {

            // Delete expired context
            if (existing != null && isExpired(existing, now)) {
                store.remove(k);
                existing = null;
            }

            // Create new context if missing or expired
            if (existing == null) {
                return new Context(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );
            }

            // Refresh last access time
            existing.touch();
            return existing;
        });
    }

    private boolean isExpired(Context context, long now) {
        return (now - context.lastAccessTime()) > EXPIRY_MS;
    }
}
