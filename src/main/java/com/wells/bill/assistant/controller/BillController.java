package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.BillCreateRequest;
import com.wells.bill.assistant.model.BillCreateResponse;
import com.wells.bill.assistant.model.BillSummary;
import com.wells.bill.assistant.model.BillUpdateRequest;
import com.wells.bill.assistant.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<BillCreateResponse> createBill(@RequestBody BillCreateRequest req) {
        BillCreateResponse billCreateResponse = billService.createBill(req);
        return ResponseEntity.ok(billCreateResponse);
    }

    // -----------------------------
    // Get Bill by ID
    // -----------------------------
    @GetMapping("/{billId}")
    public ResponseEntity<BillSummary> getBill(@PathVariable UUID billId) {
        return ResponseEntity.ok(billService.getBill(billId));
    }

    // -----------------------------
    // List Bills by Status
    // -----------------------------
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BillSummary>> listByStatus(@PathVariable String status) {
        BillStatus billStatus = BillStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(billService.listByStatus(billStatus));
    }

    // -----------------------------
    // Update Bill
    // -----------------------------
    @PutMapping("/{billId}")
    public ResponseEntity<BillCreateResponse> updateBill(@PathVariable UUID billId, @RequestBody BillUpdateRequest req) {
        return ResponseEntity.ok(billService.updateBill(billId, req));
    }

    // -----------------------------
    // List Upcoming Unpaid Bills (Next 7 Days)
    // -----------------------------
    @GetMapping("/upcoming")
    public ResponseEntity<List<BillSummary>> upcoming() {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(billService.findUpcomingUnpaidBills(now, now.plusDays(7)));
    }

    // -----------------------------
    // List All Unpaid Bills
    // -----------------------------
    @GetMapping("/unpaid")
    public ResponseEntity<List<BillSummary>> unpaid() {
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
}
