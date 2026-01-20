package com.wells.bill.assistant.model;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillStatus;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BillCreateRequest {
    @NotNull(message = "customerId is required")
    private UUID customerId;

    @NotNull(message = "consumerName is required")
    private String consumerName;

    @NotNull(message = "consumerNumber is required")
    private String consumerNumber;

    @NotNull(message = "fileName is required")
    private String fileName;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @NotNull(message = "currency is required")
    private String currency;

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;

    private String vendor;
    private BillStatus status;
    private BillCategory category;
    private LocalDate autoPayScheduledDate;
    private Boolean autoPayEnabled;
    private String extractedText;
    private String source;
}
