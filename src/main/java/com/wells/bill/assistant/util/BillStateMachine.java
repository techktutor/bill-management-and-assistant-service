package com.wells.bill.assistant.util;

import com.wells.bill.assistant.model.BillStatus;

import java.util.Map;
import java.util.Set;

public final class BillStateMachine {

    private static final Map<BillStatus, Set<BillStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    BillStatus.UPLOADED, Set.of(
                            BillStatus.INGESTING,
                            BillStatus.INGESTED,
                            BillStatus.FAILED,
                            BillStatus.CANCELLED
                    ),
                    BillStatus.INGESTED, Set.of(
                            BillStatus.VERIFIED,
                            BillStatus.FAILED,
                            BillStatus.CANCELLED
                    ),
                    BillStatus.VERIFIED, Set.of(
                            BillStatus.PAID,
                            BillStatus.OVERDUE,
                            BillStatus.CANCELLED
                    ),
                    BillStatus.OVERDUE, Set.of(
                            BillStatus.PAID,
                            BillStatus.CANCELLED
                    ),
                    BillStatus.FAILED, Set.of(
                            BillStatus.INGESTED,
                            BillStatus.CANCELLED
                    )
            );

    private BillStateMachine() {
    }

    public static void validateTransition(
            BillStatus current,
            BillStatus next
    ) {
        if (current == null) {
            throw new IllegalStateException("Current bill status is null");
        }

        Set<BillStatus> allowed = ALLOWED_TRANSITIONS.get(current);

        if (allowed == null || !allowed.contains(next)) {
            throw new IllegalStateException(
                    "Invalid bill status transition: "
                            + current + " → " + next
            );
        }
    }
}
