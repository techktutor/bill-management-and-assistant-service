package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.BillCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class BillUpdateRequest {

    private String name;

    private String vendor;

    private BillCategory category;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private BigDecimal lateFee;

    private String documentUrl;

    private String extractedText;

    private String source;
    private BigDecimal amount;
    private LocalDate dueDate;
}
