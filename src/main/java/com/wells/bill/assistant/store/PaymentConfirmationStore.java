package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.PaymentConfirmationToken;

import java.util.Optional;
import java.util.UUID;

public interface PaymentConfirmationStore {

    void save(UUID userId, PaymentConfirmationToken token);

    Optional<PaymentConfirmationToken> find(UUID userId);

    void delete(UUID userId);
}
