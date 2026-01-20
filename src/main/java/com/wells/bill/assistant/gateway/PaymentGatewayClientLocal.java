package com.wells.bill.assistant.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
     * Stores idempotent responses per gatewayIdempotencyKey.
     */
    private final Map<String, GatewayResponse> idempotencyStore = new ConcurrentHashMap<>();

    @Override
    public GatewayResponse charge(
            BigDecimal amount,
            String customerId,
            String billId,
            String gatewayIdempotencyKey,
            String cardToken,
            Map<String, Object> metadata
    ) {
        log.info("[LOCAL-GW] charge(amount={}, customerId={}, billId={}, idem={}, cardToken={}, metadata={})",
                amount, customerId, billId, gatewayIdempotencyKey, cardToken, metadata);

        // ---------- Idempotency Handling ----------
        if (gatewayIdempotencyKey != null && idempotencyStore.containsKey(gatewayIdempotencyKey)) {
            GatewayResponse cached = idempotencyStore.get(gatewayIdempotencyKey);
            log.info("[LOCAL-GW] Idempotent replay → returning cached result: key={}, reference={}",
                    gatewayIdempotencyKey, cached.reference());
            return cached;
        }

        // ---------- Simulated Failure Case ----------
        if (amount != null && amount.compareTo(BigDecimal.valueOf(13.13)) == 0) {
            GatewayResponse fail = new GatewayResponse(false, "LOCAL_FAIL_13.13_TEST");
            if (gatewayIdempotencyKey != null) idempotencyStore.put(gatewayIdempotencyKey, fail);
            log.warn("[LOCAL-GW] Simulated failure for amount 13.13");
            return fail;
        }

        // ---------- Simulated Success Case ----------
        String reference = "local_txn_" + UUID.randomUUID();
        GatewayResponse success = new GatewayResponse(true, reference);

        if (gatewayIdempotencyKey != null) idempotencyStore.put(gatewayIdempotencyKey, success);

        log.info("[LOCAL-GW] Payment success → reference={}", reference);
        return success;
    }
}
