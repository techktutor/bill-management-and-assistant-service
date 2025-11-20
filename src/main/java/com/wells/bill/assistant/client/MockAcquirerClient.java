package com.wells.bill.assistant.client;

import com.wells.bill.assistant.entity.AcquirerResponse;
import org.springframework.stereotype.Component;

@Component
public class MockAcquirerClient implements AcquirerClient {

    @Override
    public AcquirerResponse authorize(String token, Long amount, String currency, String paymentId) {
        return new AcquirerResponse(true, "AUTH-" + paymentId);
    }


    @Override
    public AcquirerResponse capture(String remoteAuthId, Long amount) {
        return new AcquirerResponse(true, remoteAuthId);
    }
}
