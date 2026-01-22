package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    /* =====================================================
     * 1️⃣ Bill Detail
     * ===================================================== */

    @GetMapping("/{billId}")
    public ResponseEntity<BillDetail> getBill(@PathVariable UUID billId) {
        return ResponseEntity.ok(billService.getBill(billId));
    }

    /* =====================================================
     * 2️⃣ List Bills (Detail DTO, paged)
     * ===================================================== */

    @GetMapping
    public ResponseEntity<Page<BillDetail>> listBills(
            @RequestParam UUID userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                billService.getBills(userId, pageable)
        );
    }

    /* =====================================================
     * 3️⃣ List Unpaid Bills
     * ===================================================== */

    @GetMapping("/unpaid")
    public ResponseEntity<List<BillDetail>> getUnpaidBills(
            @RequestParam UUID userId
    ) {
        return ResponseEntity.ok(
                billService.getUnpaidBills(userId)
        );
    }

    /* =====================================================
     * 4️⃣ Create Bill
     * ===================================================== */

    @PostMapping
    public ResponseEntity<BillDetail> createBill(
            @RequestBody BillDetail request
    ) {
        return ResponseEntity.ok(
                billService.createBill(request)
        );
    }

    /* =====================================================
     * 5️⃣ Update Bill
     * ===================================================== */

    @PutMapping("/{billId}")
    public ResponseEntity<BillDetail> updateBill(
            @PathVariable UUID billId,
            @RequestBody BillDetail request
    ) {
        return ResponseEntity.ok(
                billService.updateBill(billId, request)
        );
    }

    /* =====================================================
     * 6️⃣ Delete Bill
     * ===================================================== */

    @DeleteMapping("/{billId}")
    public ResponseEntity<Void> deleteBill(@PathVariable UUID billId) {
        billService.deleteBill(billId);
        return ResponseEntity.noContent().build();
    }

    /* =====================================================
     * 7️⃣ Scheduler / System
     * ===================================================== */

    @PostMapping("/overdue/run")
    public ResponseEntity<String> runOverdueUpdate() {
        billService.updateOverdue();
        return ResponseEntity.ok("Overdue bills updated");
    }
}

