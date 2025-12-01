package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.BillCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BillCreateRequest {
    private UUID customerId;
    private String name;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String vendor;
    private BillCategory category;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal lateFee;
    private String documentUrl;
    private LocalDate autoPayScheduledDate;
    private Boolean autoPayEnabled;
    private String extractedText;
    private String source;
}
