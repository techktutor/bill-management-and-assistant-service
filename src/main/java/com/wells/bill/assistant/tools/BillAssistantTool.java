package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.model.BillSummary;
import com.wells.bill.assistant.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * -------------------------------------------------------------------------
 * BillAssistantTool (UNIFIED BILL TOOL)
 * <p>
 * Updated to align with the new domain model and services.
 * - validates payment before marking bill paid
 * - uses enums and BigDecimal correctly
 * - adds RAG guardrails (length / similarity threshold)
 * - fixes various logic bugs from prior iteration
 * -------------------------------------------------------------------------
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillAssistantTool {

    private final BillService billService;

    // -----------------------------
    // A: BILL READ-ONLY QUERIES
    // -----------------------------
    @Tool(name = "getBillById", description = "Retrieve bill details by bill ID")
    public BillSummary getBillById(@ToolParam(description = "Bill ID") UUID billId) {
        return billService.getBill(billId);
    }

    @Tool(name = "listPendingBills", description = "List all unpaid bills (PENDING or OVERDUE) for the user")
    public List<BillSummary> listPendingBills() {
        return billService.findAllUnpaid();
    }

    @Tool(name = "dueBillsNext7Days", description = "Bills due in next 7 days.")
    public List<BillSummary> dueBillsNext7Days() {
        return billService.findByDueDateRange(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Tool(name = "findUpcomingUnpaidBills", description = "Unpaid bills due in next 7 days.")
    public List<BillSummary> findUpcomingUnpaidBills() {
        return billService.findUnpaidByDueDateRange(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Tool(name = "groupUnpaidBillsByVendor", description = "Group unpaid bills by vendor")
    public Map<String, List<BillSummary>> groupUnpaidBillsByVendor() {
        return billService.findAllUnpaid()
                .stream()
                .collect(Collectors.groupingBy(
                        b -> Optional.ofNullable(b.vendor()).orElse("unknown")
                ));
    }
}
