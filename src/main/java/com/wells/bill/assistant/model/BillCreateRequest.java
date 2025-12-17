package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BillCreateRequest {
    private UUID customerId;
    private String consumerName;
    private String consumerNumber;
    private String fileName;
    private BigDecimal amount;
    private String currency;
    private LocalDate dueDate;
    private String vendor;
    private BillStatus status;
    private BillCategory category;
    private LocalDate autoPayScheduledDate;
    private Boolean autoPayEnabled;
    private String extractedText;
    private String source;
}
