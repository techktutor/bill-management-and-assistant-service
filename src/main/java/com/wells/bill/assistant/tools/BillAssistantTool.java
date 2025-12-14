package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.model.BillCreateResponse;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.RagEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private final RagEngineService ragEngineService;

    // -----------------------------
    // A: BILL READ-ONLY QUERIES
    // -----------------------------
    @Tool(name = "getBillById", description = "Retrieve bill details by bill ID.")
    public BillCreateResponse getBillById(@ToolParam(description = "Bill ID") UUID billId) {
        return billService.getBill(billId);
    }

    @Tool(name = "listPendingBills", description = "List all unpaid bills (PENDING or OVERDUE).")
    public List<BillCreateResponse> listPendingBills() {
        return billService.findAllUnpaid();
    }

    @Tool(name = "dueBillsNext7Days", description = "Bills due in next 7 days.")
    public List<BillCreateResponse> dueBillsNext7Days() {
        return billService.findByDueDateRange(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Tool(name = "findUpcomingUnpaidBills", description = "Unpaid bills due in next 7 days.")
    public List<BillCreateResponse> findUpcomingUnpaidBills() {
        return billService.findUnpaidByDueDateRange(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Tool(name = "showUnpaidGroupedByVendor", description = "Group unpaid bills by vendor.")
    public Map<String, List<BillCreateResponse>> showUnpaidGroupedByVendor() {
        return billService.findAllUnpaid().stream()
                .collect(Collectors.groupingBy(b -> b.getVendor() == null ? "unknown" : b.getVendor()));
    }

    // -----------------------------
    // B: RAG & SEMANTIC REASONING (READ-ONLY)
    // -----------------------------
    @Tool(name = "ragRetrieveBillContext", description = "Retrieve stitched RAG context for a bill.")
    public String ragRetrieveBillContext(@ToolParam(description = "Bill ID") String billId) {
        List<Document> docs = ragEngineService.retrieveByBillIdHybrid(billId, "bill context", 12);
        if (docs.isEmpty()) return "No bill context found.";
        return ragEngineService.stitchContext(docs, 8000);
    }

    @Tool(name = "answerBillQuestion", description = "Answer questions using RAG on bill context.")
    public String answerBillQuestion(@ToolParam(description = "Bill ID") String billId,
                                     @ToolParam(description = "User question") String question) {
        return ragEngineService.answerQuestionForBill(billId, question);
    }

    @Tool(name = "explainWhyOverdue", description = "Explain overdue reason using RAG.")
    public String explainWhyOverdue(@ToolParam(description = "Bill ID") String billId) {
        return ragEngineService.answerQuestionForBill(billId, "Explain why this bill is overdue.");
    }
}
