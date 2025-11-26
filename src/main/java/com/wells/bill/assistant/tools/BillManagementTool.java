package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BillManagementTool {

    private final BillService billService;

    @Tool(name = "getBillById",
            description = "Retrieve bill details by bill ID.")
    public BillEntity getBill(
            @ToolParam(description = "Bill ID") Long billId
    ) {
        return billService.getBill(billId);
    }

    @Tool(name = "listPendingBills",
            description = "List all bills with status PENDING or OVERDUE.")
    public List<BillEntity> listPending() {
        return billService.listByStatus("PENDING");
    }

    @Tool(name = "markBillPaid",
            description = "Mark the specified bill as PAID.")
    public String markPaid(
            @ToolParam(description = "Bill ID") Long billId
    ) {
        billService.markAsPaid(billId);
        return "Bill marked as PAID: " + billId;
    }
}
