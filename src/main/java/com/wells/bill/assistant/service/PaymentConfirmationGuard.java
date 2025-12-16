package com.wells.bill.assistant.service;

import com.wells.bill.assistant.store.ConversationStateStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PaymentConfirmationGuard
 * -------------------------
 * Centralized, deterministic guard for payment confirmation logic.
 * <p>
 * Responsibilities:
 * - Detect payment intent (cheap heuristic)
 * - Extract billId deterministically from user input
 * - Enforce per-bill confirmation
 * - Enforce TTL on confirmation
 * - Decide whether payment tools may be exposed
 * <p>
 * This class contains NO LLM logic and NO side effects beyond stateStore writes.
 */
@Component
public class PaymentConfirmationGuard {

    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(5);

    private static final Pattern BILL_ID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    private final ConversationStateStore stateStore;

    public PaymentConfirmationGuard(@Qualifier("inMemoryConversationStateStore") ConversationStateStore stateStore) {
        this.stateStore = stateStore;
    }

    public GuardResult evaluate(String conversationId, String userMessage) {
        boolean confirmation = isConfirmation(userMessage);
        boolean paymentIntent = isPaymentIntent(userMessage);
        UUID billIdFromMsg = extractBillId(userMessage);

        // ------------------------------------------------------------
        // 1️⃣ Confirmation ONLY if there is a pending confirmation
        // ------------------------------------------------------------
        if (confirmation && billIdFromMsg != null) {
            String key = confirmationKey(billIdFromMsg);
            String state = stateStore.get(conversationId, key);

            if ("PENDING".equals(state)) {
                stateStore.put(conversationId, key, "CONFIRMED", CONFIRMATION_TTL);
                return GuardResult.confirmed(billIdFromMsg);
            }
            // ❗ otherwise: treat as normal input
        }

        // ------------------------------------------------------------
        // 2️⃣ Normal payment intent flow
        // ------------------------------------------------------------
        if (!paymentIntent) {
            return GuardResult.noPayment();
        }

        if (billIdFromMsg == null) {
            return GuardResult.missingBill();
        }

        String key = confirmationKey(billIdFromMsg);
        String state = stateStore.get(conversationId, key);

        if (state == null) {
            stateStore.put(conversationId, key, "PENDING", CONFIRMATION_TTL);
            return GuardResult.confirmationRequired(billIdFromMsg);
        }

        if ("CONFIRMED".equals(state)) {
            return GuardResult.confirmed(billIdFromMsg);
        }

        return GuardResult.confirmationRequired(billIdFromMsg);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private boolean isPaymentIntent(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("pay") || m.contains("payment") || m.contains("schedule");
    }

    private boolean isConfirmation(String msg) {
        if (msg == null) return false;
        String m = msg.trim().toLowerCase();
        return m.startsWith("yes") || m.equals("confirm") || m.equals("confirmed");
    }

    private UUID extractBillId(String msg) {
        if (msg == null) return null;
        Matcher matcher = BILL_ID_PATTERN.matcher(msg);
        return matcher.find() ? UUID.fromString(matcher.group()) : null;
    }

    private String confirmationKey(UUID billId) {
        return "payment_confirmation:" + billId;
    }

    // ------------------------------------------------------------------
    // Result model
    // ------------------------------------------------------------------
    public record GuardResult(
            boolean paymentIntent,
            boolean confirmed,
            UUID billId,
            String userMessage
    ) {
        static GuardResult noPayment() {
            return new GuardResult(false, false, null, null);
        }

        static GuardResult missingBill() {
            return new GuardResult(
                    true,   // user intends to pay
                    false,
                    null,
                    "Please specify which bill you want to pay."
            );
        }

        static GuardResult confirmationRequired(UUID billId) {
            return new GuardResult(
                    true,   // ✅ THIS IS THE FIX
                    false,
                    billId,
                    "You are about to make a payment for bill " + billId + ". Please confirm to proceed."
            );
        }

        static GuardResult confirmed(UUID billId) {
            return new GuardResult(
                    false,  // confirmation itself is not a new intent
                    true,
                    billId,
                    null
            );
        }
    }
}
