package com.wells.bill.assistant.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ExternalPaymentGatewayClient defines the contract for interacting with
 * real or simulated payment gateways (Stripe, Razorpay, Adyen, etc.).
 * <p>
 * This interface is intentionally minimal and environment‑agnostic.
 * <p>
 * For development/local usage, a LocalPaymentGatewayClient implementation
 * can simulate charges, handle idempotency, and return deterministic results.
 * <p>
 * Production implementations should:
 * - use HTTPS calls
 * - implement true idempotency keys
 * - return gateway transaction references
 * - never store raw card details
 */
public interface PaymentGatewayClient {

    /**
     * Response payload from a gateway operation.
     *
     * @param reference transactionId, errorMessage, etc.
     */
    record GatewayResponse(boolean success, String reference) {

    }

    /**
     * Charge a customer.
     *
     * @param amount                Payment amount
     * @param customerId            UUID (string formatted)
     * @param billId                Bill identifier
     * @param gatewayIdempotencyKey Idempotency key for the external provider
     * @param cardToken             Tokenized card (tok_xxx, card_xxx)
     * @param metadata              Additional metadata for the gateway
     * @return GatewayResponse
     */
    GatewayResponse charge(
            BigDecimal amount,
            String customerId,
            String billId,
            String gatewayIdempotencyKey,
            String cardToken,
            Map<String, Object> metadata
    );
}
