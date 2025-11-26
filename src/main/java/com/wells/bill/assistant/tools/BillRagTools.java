package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.repository.BillRepository;
import com.wells.bill.assistant.service.VectorRetrievalService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BillRagTools {

    private final VectorRetrievalService retrievalService;
    private final BillRepository billRepository;

    public BillRagTools(VectorRetrievalService retrievalService, BillRepository billRepository) {
        this.retrievalService = retrievalService;
        this.billRepository = billRepository;
    }

    @Tool(name = "ragRetrieveBillContext", description = "Retrieve semantic context chunks for a bill (billId)")
    public List<String> ragRetrieveBillContext(@ToolParam(description = "Bill ID") String billId) {
        List<Document> docs = retrievalService.retrieveByBillId(billId, 12);
        return docs.stream().map(Document::getText).collect(Collectors.toList());
    }

    @Tool(name = "showUnpaidGroupedByVendor", description = "Return unpaid bills grouped by vendor")
    public Map<String, List<BillEntity>> showUnpaidGroupedByVendor() {
        LocalDate today = LocalDate.now();
        List<BillEntity> unpaid = billRepository.findAll().stream()
                .filter(b -> b.getStatus() != null && (b.getStatus().equalsIgnoreCase("PENDING") || b.getStatus().equalsIgnoreCase("OVERDUE")))
                .toList();

        return unpaid.stream().collect(Collectors.groupingBy(b -> b.getVendor() == null ? "unknown" : b.getVendor()));
    }
}
