package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    @PostMapping
    public ResponseEntity<BillEntity> create(@RequestBody BillEntity bill) {
        return ResponseEntity.ok(billService.createBill(bill));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillEntity> get(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBill(id));
    }

    @GetMapping
    public ResponseEntity<List<BillEntity>> list() {
        return ResponseEntity.ok(billService.listAllBills());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BillEntity>> listByStatus(@PathVariable String status) {
        return ResponseEntity.ok(billService.listByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillEntity> update(
            @PathVariable Long id,
            @RequestBody BillEntity bill
    ) {
        return ResponseEntity.ok(billService.updateBill(id, bill));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        billService.deleteBill(id);
        return ResponseEntity.noContent().build();
    }
}
