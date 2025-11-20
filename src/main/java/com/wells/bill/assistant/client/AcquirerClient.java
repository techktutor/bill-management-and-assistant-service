package com.wells.bill.assistant.client;

import com.wells.bill.assistant.entity.AcquirerResponse;

public interface AcquirerClient {
    AcquirerResponse authorize(String token, Long amount, String currency, String paymentId);
    AcquirerResponse capture(String remoteAuthId, Long amount);
}
