package com.wells.bill.assistant.service;

import com.wells.bill.assistant.exception.InvalidPaymentStateException;
import com.wells.bill.assistant.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentGuardService {

    @Value("${payment.confirmation.ttl.seconds:120}")
    private long ttlSeconds;

    public GuardResult evaluate(ConversationContext context, Intent intent) {
        if (intent instanceof UnknownIntent) {
            return GuardResult.allow();
        }

        if (intent instanceof InitiatePaymentIntent initiate) {
            return handleInitiation(context, initiate);
        }

        if (intent instanceof SchedulePaymentIntent initiate) {
            PendingPayment pending = new PendingPayment(initiate.billId(), initiate.amount(), initiate.scheduledDate());
            return handleInitiation(context, pending);
        }

        if (intent instanceof ConfirmPaymentIntent) {
            return handleConfirmation(context);
        }

        return GuardResult.allow();
    }

    private GuardResult handleInitiation(ConversationContext context, PendingPayment pending) {
        if (context.state() != ConversationState.IDLE) {
            return GuardResult.block("A payment is already in progress.");
        }
        return GuardResult.requestConfirmation("Do you want to proceed with the payment?", pending, ConversationState.AWAITING_CONFIRMATION);
    }

    private GuardResult handleInitiation(ConversationContext context, InitiatePaymentIntent intent) {
        if (context.state() != ConversationState.IDLE) {
            return GuardResult.block("A payment is already in progress.");
        }

        PendingPayment pending = new PendingPayment(intent.billId(), intent.amount(), null);
        return GuardResult.requestConfirmation("Do you want to proceed with the payment?", pending, ConversationState.AWAITING_CONFIRMATION);
    }

    private GuardResult handleConfirmation(ConversationContext context) {
        if (context.state() != ConversationState.AWAITING_CONFIRMATION) {
            return GuardResult.block("There is no payment awaiting confirmation.");
        }

        if (context.isConfirmationExpired(ttlSeconds)) {
            return GuardResult.block("Payment confirmation expired. Please start again.");
        }

        return GuardResult.allowAndTransition(ConversationState.PAYMENT_INTENT_ALLOWED);
    }

    public void validateExecution(ConversationContext context) {
        if (context.state() != ConversationState.EXECUTING_PAYMENT) {
            throw new InvalidPaymentStateException("Cannot execute payment in state " + context.state());
        }
    }
}
