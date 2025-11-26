package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BillQueryTools {

    private final BillService billService;
    private final PaymentHistoryService paymentHistoryService;

    @Tool(name = "dueBillsNext7Days", description = "List bills due in the next 7 days.")
    public List<BillEntity> dueBillsNext7Days() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(7);
        return billService.findByDueDateRange(today, end);
    }

    @Tool(name = "findUpcomingUnpaidBills", description = "List Upcoming Unpaid Bills due in the next 7 days.")
    public List<BillEntity> findUpcomingUnpaidBills() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(7);
        return billService.findByDueDateRange(today, end);
    }

    @Tool(name = "whyIsBillOverdue", description = "Explain why a bill is overdue using bill data.")
    public String whyIsBillOverdue(@ToolParam(description = "Bill ID") Long billId) {
        BillEntity bill = billService.getBill(billId);
        if (!"OVERDUE".equalsIgnoreCase(bill.getStatus())) {
            return "Bill is not overdue.";
        }
        // Basic reasons - this can be enriched using RAG context
        return String.format("Bill %s is overdue. Amount: $%.2f, Due date: %s", bill.getName(), bill.getAmount(), bill.getDueDate());
    }

    @Tool(name = "howMuchPaidLastMonth", description = "Sum of payments made by a customer in the previous month.")
    public String howMuchPaidLastMonth(@ToolParam(description = "Customer UUID") String customerIdStr) {
        UUID customerId = UUID.fromString(customerIdStr);
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        var payments = paymentHistoryService.findPaymentsByCustomer(customerId);
        long sumCents = payments.stream()
                .filter(p -> YearMonth.from(p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()).equals(lastMonth))
                .mapToLong(PaymentEntity::getAmount)
                .sum();
        return String.format("Total paid in %s: $%.2f", lastMonth, sumCents / 100.0);
    }
}
