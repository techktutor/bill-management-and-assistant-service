package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.PaymentConfirmationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryPaymentConfirmationStore implements PaymentConfirmationStore {

    private final ConcurrentMap<String, PaymentConfirmationToken> store =
            new ConcurrentHashMap<>();

    @Override
    public void save(PaymentConfirmationToken token) {
        store.put(token.token(), token);
    }

    @Override
    public Optional<PaymentConfirmationToken> find(String token) {
        PaymentConfirmationToken stored = store.get(token);

        if (stored == null) {
            return Optional.empty();
        }

        // Expiry check
        if (stored.expiresAt().isBefore(Instant.now())) {
            store.remove(token);
            return Optional.empty();
        }

        return Optional.of(stored);
    }

    @Override
    public void delete(String token) {
        store.remove(token);
    }
}

