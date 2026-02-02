package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.store.ContextStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.wells.bill.assistant.util.CookieGenerator.*;

@Slf4j
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final ContextStore contextStore;

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
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

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
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

        log.info("Listing bills for user: {}", userId);

        return ResponseEntity.ok(
                billService.getBills(context.userId(), pageable)
        );
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
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

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
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

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
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

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
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

        billService.updateOverdue();
        return ResponseEntity.ok("Overdue bills updated");
    }
}
