package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.ContextFacade;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.wells.bill.assistant.util.CookieGenerator.CONTEXT_COOKIE;
import static com.wells.bill.assistant.util.CookieGenerator.USER_COOKIE;

@Slf4j
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final ContextFacade contextFacade;

    /* =====================================================
     * 1️⃣ Bill Detail
     * ===================================================== */

    @GetMapping("/{billId}")
    public ResponseEntity<BillDetail> getBill(
            @PathVariable UUID billId,
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        Context context = contextFacade.resolveContext(rawContextId, rawUserId, response);

        return ResponseEntity.ok(billService.getBill(billId, context.userId()));
    }

    /* =====================================================
     * 2️⃣ List Bills (Detail DTO, paged)
     * ===================================================== */

    @GetMapping
    public ResponseEntity<Page<BillDetail>> listBills(
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response,
            Pageable pageable
    ) {
        Context context = contextFacade.resolveContext(rawContextId, rawUserId, response);

        log.info("Listing bills for user: {}", context.userId());

        return ResponseEntity.ok(billService.getBills(context.userId(), pageable));
    }

    /* =====================================================
     * 3️⃣ List Unpaid Bills
     * ===================================================== */

    @GetMapping("/unpaid")
    public ResponseEntity<List<BillDetail>> getUnpaidBills(
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        Context context = contextFacade.resolveContext(rawContextId, rawUserId, response);

        return ResponseEntity.ok(
                billService.getUnpaidBills(context.userId())
        );
    }

    /* =====================================================
     * 4️⃣ Update Bill
     * ===================================================== */

    @PutMapping("/{billId}")
    public ResponseEntity<BillDetail> updateBill(
            @PathVariable UUID billId,
            @RequestBody BillDetail request,
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        Context context = contextFacade.resolveContext(rawContextId, rawUserId, response);

        return ResponseEntity.ok(
                billService.updateBill(billId, context.userId(), request)
        );
    }

    /* =====================================================
     * 5️⃣ Delete Bill
     * ===================================================== */

    @DeleteMapping("/{billId}")
    public ResponseEntity<Void> deleteBill(
            @PathVariable UUID billId,
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        Context context = contextFacade.resolveContext(rawContextId, rawUserId, response);

        billService.deleteBill(billId, context.userId());
        return ResponseEntity.noContent().build();
    }

    /* =====================================================
     * 6️⃣ Scheduler / System
     * ===================================================== */

    @PostMapping("/overdue/run")
    public ResponseEntity<String> runOverdueUpdate(
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        Context context = contextFacade.resolveContext(rawContextId, rawUserId, response);

        billService.updateOverdue();
        return ResponseEntity.ok("Overdue bills updated");
    }
}
