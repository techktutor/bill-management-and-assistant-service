package com.wells.bill.assistant.gateway;

import com.wells.bill.assistant.model.GatewayRequest;
import com.wells.bill.assistant.model.GatewayResponse;

public interface PaymentGatewayClient {

    GatewayResponse charge(GatewayRequest request);
}
