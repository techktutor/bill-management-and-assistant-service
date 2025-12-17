package com.wells.bill.assistant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillDetails {
    @JsonIgnore
    private String fileName;
    private String consumerName;
    private String consumerNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate lastDueDate;
}
