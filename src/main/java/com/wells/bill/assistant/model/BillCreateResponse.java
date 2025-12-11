package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BillCreateResponse {

    private Long id;

    private UUID customerId;

    private String name;

    private BigDecimal amount;

    private LocalDate dueDate;

    private BillStatus status;

    private String vendor;

    private BillCategory category;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private BigDecimal lateFee;

    private String paymentId;

    private String lastSuccessfulPaymentId;

    private String documentUrl;

    private String extractedText;

    private String source;

    private Boolean autoPayEnabled;

    private LocalDate autoPayScheduledDate;

    private String autoPayPaymentId;

    private Instant lastAutoPayRun;

    private Boolean deleted;

    private String referenceNumber;

    private String shortDescription;

    private Instant createdAt;

    private Instant updatedAt;
}
