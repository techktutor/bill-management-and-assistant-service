package com.wells.bill.assistant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wells.bill.assistant.entity.BillCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class BillDetails {
    @JsonIgnore
    private String fileName;
    private String consumerName;
    private String consumerNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate lastDueDate;
    private UUID customerId;
    private String currency;
    private String vendor;
    private BillCategory category;
    private Boolean autoPayEnabled;
}
