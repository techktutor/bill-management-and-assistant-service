package com.wells.bill.assistant.store;

import com.wells.bill.assistant.entity.ContextEntity;
import com.wells.bill.assistant.exception.InvalidContextException;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.repository.ContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContextStore {

    private static final long EXPIRY_MS = 10 * 60 * 1000;

    private final ContextRepository repository;

    /* =========================
     * HTTP path (browser)
     * ========================= */
    public Context getOrCreateByContextKey(String contextKey, UUID userId) {
        long now = System.currentTimeMillis();

        return repository.findById(contextKey)
                .map(entity -> {
                    if (isExpired(entity, now)) {
                        repository.delete(entity);
                        return createNewContext(contextKey, userId);
                    }
                    entity.touch();
                    repository.save(entity);
                    return toContext(entity);
                })
                .orElseGet(() -> createNewContext(contextKey, userId));
    }

    /* =========================
     * Internals
     * ========================= */
    private Context createNewContext(String contextKey, UUID userId) {
        ContextEntity entity = new ContextEntity(
                contextKey,
                userId,
                UUID.randomUUID()
        );
        repository.save(entity);
        return toContext(entity);
    }

    private boolean isExpired(ContextEntity entity, long now) {
        return (now - entity.getLastAccessTime()) > EXPIRY_MS;
    }

    private Context toContext(ContextEntity entity) {
        return new Context(
                entity.getUserId(),
                entity.getConversationId()
        );
    }
}
