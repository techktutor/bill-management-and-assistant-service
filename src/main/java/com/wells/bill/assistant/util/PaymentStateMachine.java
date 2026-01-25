package com.wells.bill.assistant.util;

import com.wells.bill.assistant.model.PaymentStatus;

import java.util.Map;
import java.util.Set;

public final class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    PaymentStatus.CREATED, Set.of(
                            PaymentStatus.APPROVAL_PENDING,
                            PaymentStatus.APPROVED,
                            PaymentStatus.CANCELLED
                    ),
                    PaymentStatus.SCHEDULED, Set.of(
                            PaymentStatus.APPROVAL_PENDING,
                            PaymentStatus.APPROVED,
                            PaymentStatus.CANCELLED
                    ),
                    PaymentStatus.APPROVAL_PENDING, Set.of(
                            PaymentStatus.APPROVED,
                            PaymentStatus.REJECTED
                    ),
                    PaymentStatus.APPROVED, Set.of(
                            PaymentStatus.PROCESSING
                    ),
                    PaymentStatus.PROCESSING, Set.of(
                            PaymentStatus.SUCCESS,
                            PaymentStatus.FAILED
                    ),
                    PaymentStatus.REJECTED, Set.of(
                            PaymentStatus.CANCELLED
                    )
            );

    private PaymentStateMachine() {
    }

    public static void validateTransition(
            PaymentStatus current,
            PaymentStatus next
    ) {
        if (current == null) {
            throw new IllegalStateException("Current payment status is null");
        }

        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.get(current);

        if (allowed == null || !allowed.contains(next)) {
            throw new IllegalStateException(
                    "Invalid payment status transition: "
                            + current + " → " + next
            );
        }
    }
}
