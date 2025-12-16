package com.wells.bill.assistant.service;

import com.wells.bill.assistant.store.InMemoryConversationStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentConfirmationGuardTest {

    private PaymentConfirmationGuard guard;

    private final String conversationId = "conv-1";

    @BeforeEach
    void setup() {
        InMemoryConversationStateStore stateStore = new InMemoryConversationStateStore();
        guard = new PaymentConfirmationGuard(stateStore);
    }

    @Test
    void noPaymentIntent_shouldNotTriggerGuard() {
        var result = guard.evaluate(conversationId, "Show my bills");

        assertFalse(result.paymentIntent());
        assertFalse(result.confirmed());
        assertNull(result.billId());
        assertNull(result.userMessage());
    }

    @Test
    void paymentIntentWithoutBill_shouldAskForBill() {
        var result = guard.evaluate(conversationId, "Pay my bill");

        assertTrue(result.paymentIntent());
        assertFalse(result.confirmed());
        assertNull(result.billId());
        assertEquals("Please specify which bill you want to pay.", result.userMessage());
    }

    @Test
    void firstPaymentIntent_shouldRequestConfirmation() {
        UUID billId = UUID.randomUUID();
        String msg = "Pay bill " + billId;

        var result = guard.evaluate(conversationId, msg);

        assertTrue(result.paymentIntent());
        assertFalse(result.confirmed());
        assertEquals(billId, result.billId());
        assertTrue(result.userMessage().contains("Please confirm"));
    }

    @Test
    void confirmation_shouldAllowPayment() {
        UUID billId = UUID.randomUUID();
        String payMsg = "Pay bill " + billId;

        guard.evaluate(conversationId, payMsg); // request confirmation

        // Explicit confirmation with bill context
        var confirm = guard.evaluate(conversationId, "yes, pay bill " + billId);

        assertFalse(confirm.paymentIntent());
        assertTrue(confirm.confirmed());
        assertEquals(billId, confirm.billId());
        assertNull(confirm.userMessage());
    }

    @Test
    void confirmationIsPerBill() {
        UUID billA = UUID.randomUUID();
        UUID billB = UUID.randomUUID();

        guard.evaluate(conversationId, "Pay bill " + billA);
        guard.evaluate(conversationId, "yes");

        var other = guard.evaluate(conversationId, "Pay bill " + billB);

        assertFalse(other.confirmed());
        assertEquals(billB, other.billId());
        assertTrue(other.userMessage().contains("Please confirm"));
    }

    @Test
    void confirmationExpires_shouldRequireReconfirmation() throws InterruptedException {
        // Short TTL override via reflection or constructor if you later inject TTL
        UUID billId = UUID.randomUUID();

        guard.evaluate(conversationId, "Pay bill " + billId);
        guard.evaluate(conversationId, "yes");

        // simulate expiry
        Thread.sleep(Duration.ofMinutes(1).toMillis());

        var result = guard.evaluate(conversationId, "Pay bill " + billId);

        assertFalse(result.confirmed());
        assertTrue(result.userMessage().contains("Please confirm"));
    }

    @Test
    void confirmation_shouldNotConfirmDifferentBill() {
        UUID billA = UUID.randomUUID();
        UUID billB = UUID.randomUUID();

        // Step 1: User initiates payment for bill A
        guard.evaluate(conversationId, "Pay bill " + billA);

        // Step 2: User confirms but mentions bill B
        var result = guard.evaluate(conversationId, "yes, pay bill " + billB);

        // ❌ Must NOT confirm bill B
        assertFalse(result.confirmed());
        assertTrue(result.paymentIntent()); // still intent
        assertEquals(billB, result.billId()); // confirmationRequired for B
        assertTrue(result.userMessage().contains("Please confirm"));
    }

    @Test
    void confirmation_withoutPendingIntent_shouldDoNothing() {
        var result = guard.evaluate(conversationId, "yes");

        assertFalse(result.paymentIntent());
        assertFalse(result.confirmed());
        assertNull(result.billId());
        assertNull(result.userMessage());
    }
}
