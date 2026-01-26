package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.PaymentConfirmationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PaymentConfirmationStoreInMemory implements PaymentConfirmationStore {

    private final ConcurrentMap<UUID, PaymentConfirmationToken> store = new ConcurrentHashMap<>();

    @Override
    public void save(UUID userId, PaymentConfirmationToken token) {
        store.put(userId, token);
    }

    @Override
    public Optional<PaymentConfirmationToken> find(UUID userId) {
        PaymentConfirmationToken stored = store.get(userId);

        if (stored == null) {
            return Optional.empty();
        }

        // Expiry check
        if (stored.expiresAt().isBefore(Instant.now())) {
            store.remove(userId);
            return Optional.empty();
        }

        return Optional.of(stored);
    }

    @Override
    public void delete(UUID userId) {
        store.remove(userId);
    }
}

