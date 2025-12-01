package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.BillCreateRequest;
import com.wells.bill.assistant.model.BillUpdateRequest;
import com.wells.bill.assistant.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    // -----------------------------
    // Create Bill
    // -----------------------------
    @PostMapping
    public ResponseEntity<BillEntity> createBill(@RequestBody BillCreateRequest req) {
        BillEntity entity = billService.createBill(req);
        return ResponseEntity.ok(entity);
    }

    // -----------------------------
    // Get Bill by ID
    // -----------------------------
    @GetMapping("/{billId}")
    public ResponseEntity<BillEntity> getBill(@PathVariable Long billId) {
        return ResponseEntity.ok(billService.getBill(billId));
    }

    // -----------------------------
    // List All Bills
    // -----------------------------
    @GetMapping
    public ResponseEntity<List<BillEntity>> listAll() {
        return ResponseEntity.ok(billService.listAllBills());
    }

    // -----------------------------
    // List Bills by Status
    // -----------------------------
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BillEntity>> listByStatus(@PathVariable String status) {
        BillStatus billStatus = BillStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(billService.listByStatus(billStatus));
    }

    // -----------------------------
    // Update Bill
    // -----------------------------
    @PutMapping("/{billId}")
    public ResponseEntity<BillEntity> updateBill(@PathVariable Long billId,
                                                 @RequestBody BillUpdateRequest req) {
        return ResponseEntity.ok(billService.updateBill(billId, req));
    }

    // -----------------------------
    // Delete Bill
    // -----------------------------
    @DeleteMapping("/{billId}")
    public ResponseEntity<Void> deleteBill(@PathVariable Long billId) {
        billService.deleteBill(billId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------
    // List Upcoming Unpaid Bills (Next 7 Days)
    // -----------------------------
    @GetMapping("/upcoming")
    public ResponseEntity<List<BillEntity>> upcoming() {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(billService.findUpcomingUnpaidBills(now, now.plusDays(7)));
    }

    // -----------------------------
    // List All Unpaid Bills
    // -----------------------------
    @GetMapping("/unpaid")
    public ResponseEntity<List<BillEntity>> unpaid() {
        return ResponseEntity.ok(billService.findAllUnpaid());
    }

    // -----------------------------
    // Force Overdue Update (Dev Only)
    // -----------------------------
    @PostMapping("/overdue/run")
    public ResponseEntity<String> runOverdueUpdate() {
        billService.updateOverdue();
        return ResponseEntity.ok("Overdue bills updated");
    }

    @PostMapping("/{billId}/markPaid")
    public ResponseEntity<String> markPaid(
            @PathVariable Long billId,
            @RequestParam(required = false) String paymentId
    ) {
        billService.markAsPaid(billId, paymentId, false);
        return ResponseEntity.ok("Bill marked as PAID");
    }

}
