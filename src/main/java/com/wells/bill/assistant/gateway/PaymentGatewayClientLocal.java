package com.wells.bill.assistant.gateway;

import com.wells.bill.assistant.model.GatewayRequest;
import com.wells.bill.assistant.model.GatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory mock implementation of ExternalPaymentGatewayClient.
 * <p>
 * Features:
 * - Deterministic idempotency: same gatewayIdempotencyKey → same response
 * - Always succeeds unless amount == 13.13 (test failure trigger)
 * - Does NOT store raw card data; cardToken is only used for logging
 * - Perfect for local/dev/test environments
 */
@Slf4j
@Component
public class PaymentGatewayClientLocal implements PaymentGatewayClient {

    /**
     * Simulates gateway-side idempotency store
     * Key: idempotencyKey
     * Value: GatewayResponse
     */
    private final Map<String, GatewayResponse> idempotencyStore = new ConcurrentHashMap<>();

    @Override
    public GatewayResponse charge(GatewayRequest request) {
        // 🔐 Gateway idempotency guarantee
        if (idempotencyStore.containsKey(request.getIdempotencyKey())) {
            return idempotencyStore.get(request.getIdempotencyKey());
        }

        // ---- Simulate payment decision ----
        GatewayResponse response;

        if (request.getAmount().signum() <= 0) {
            response = GatewayResponse.failure("INVALID_AMOUNT", "Amount must be greater than zero");
        } else {
            response = GatewayResponse.success("GW-" + UUID.randomUUID());
        }

        // Store result against idempotency key
        idempotencyStore.put(request.getIdempotencyKey(), response);

        return response;
    }
}

