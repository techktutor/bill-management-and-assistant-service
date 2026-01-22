package com.wells.bill.assistant.store;

import com.wells.bill.assistant.model.PaymentConfirmationToken;

import java.util.Optional;

public interface PaymentConfirmationStore {

    void save(PaymentConfirmationToken token);

    Optional<PaymentConfirmationToken> find(String token);

    void delete(String token);
}

