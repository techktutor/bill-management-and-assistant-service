package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class BillSummary {
    UUID id;
    String name;
    String vendor;
    BigDecimal amount;
    String currency;
    LocalDate dueDate;
    BillStatus status;
    BillCategory category;
    Boolean autoPayEnabled;

    /**
     * Factory method from BillEntity
     */
    public static BillSummary from(BillEntity bill) {
        if (bill == null) {
            return null;
        }

        return BillSummary.builder()
                .id(bill.getId())
                .name(bill.getName())
                .vendor(bill.getVendor())
                .amount(bill.getAmount())
                .currency(
                        bill.getCurrency() != null
                                ? bill.getCurrency()
                                : "USD"
                )
                .dueDate(bill.getDueDate())
                .status(bill.getStatus())
                .category(bill.getCategory())
                .autoPayEnabled(Boolean.TRUE.equals(bill.getAutoPayEnabled()))
                .build();
    }

    /**
     * Human-friendly one-line description (LLM-safe)
     */
    public String describe() {
        return String.format(
                "Bill '%s' from %s: %s %s, due on %s, status %s",
                name,
                vendor != null ? vendor : "unknown vendor",
                currency,
                amount,
                dueDate != null ? dueDate : "unknown date",
                status
        );
    }
}
