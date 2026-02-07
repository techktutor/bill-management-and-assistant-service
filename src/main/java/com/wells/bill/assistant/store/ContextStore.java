package com.wells.bill.assistant.store;

import com.wells.bill.assistant.entity.ContextEntity;
import com.wells.bill.assistant.exception.ContextMismatchException;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.repository.ContextRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class ContextStore {

    private static final long EXPIRY_MS = 10 * 60 * 1000;

    private final Clock clock;
    private final ContextRepository repository;

    public Context resolveContext(UUID contextId, UUID userId) {
        long now = clock.millis();

        ContextEntity entity = repository.findById(contextId)
                .map(existing -> refresh(existing, userId, now))
                .orElseGet(() -> create(contextId, userId, now));

        return new Context(
                entity.getContextId(),
                entity.getConversationId(),
                entity.getUserId()
        );
    }

    private ContextEntity refresh(ContextEntity entity, UUID userId, long now) {
        if (!entity.getUserId().equals(userId)) {
            log.warn("Context ownership mismatch: contextId={}", entity.getContextId());
            throw new ContextMismatchException("Context ownership mismatch");
        }

        if (isExpired(entity, now)) {
            entity.resetConversation(now);
        } else {
            entity.touch(now);
        }

        return repository.save(entity);
    }

    private ContextEntity create(UUID contextId, UUID userId, long now) {
        try {
            return repository.save(
                    new ContextEntity(contextId, userId, UUID.randomUUID(), now)
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("Context already exists, reloading: {}", contextId);
            return repository.findById(contextId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Context creation race failed for contextId=" + contextId
                    ));
        }
    }

    private boolean isExpired(ContextEntity entity, long now) {
        return now - entity.getLastAccessTime() > EXPIRY_MS;
    }
}
