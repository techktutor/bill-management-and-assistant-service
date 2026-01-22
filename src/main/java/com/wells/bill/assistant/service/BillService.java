package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.repository.BillRepository;
import com.wells.bill.assistant.util.BillMapper;
import com.wells.bill.assistant.util.BillStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BillService {

    private final BillRepository billRepository;

    /* =====================================================
     * 1️⃣ READ – Detail & Lists
     * ===================================================== */

    @Transactional(readOnly = true)
    public BillDetail getBill(UUID billId) {
        return BillMapper.toDetail(getEntityOrThrow(billId));
    }

    @Transactional(readOnly = true)
    public Page<BillDetail> getBills(UUID userId, Pageable pageable) {
        return billRepository.findByUserId(userId, pageable)
                .map(BillMapper::toDetail);
    }

    @Transactional(readOnly = true)
    public List<BillDetail> getUnpaidBills(UUID userId) {
        return billRepository.findByUserIdAndStatusIn(
                        userId,
                        Set.of(
                                BillStatus.UPLOADED,
                                BillStatus.INGESTED,
                                BillStatus.VERIFIED,
                                BillStatus.OVERDUE
                        )
                )
                .stream()
                .map(BillMapper::toDetail)
                .toList();
    }

    /* =====================================================
     * 2️⃣ CREATE / UPDATE / DELETE
     * ===================================================== */

    public BillDetail createBill(BillDetail request) {
        BillEntity entity = BillMapper.toEntity(request);

        // Enforce invariants
        entity.setId(null);
        entity.setStatus(BillStatus.UPLOADED);
        entity.setPaymentId(null);

        return BillMapper.toDetail(
                billRepository.save(entity)
        );
    }

    public BillDetail updateBill(UUID billId, BillDetail request) {
        BillEntity existing = getEntityOrThrow(billId);

        if (existing.getStatus() == BillStatus.PAID
                || existing.getStatus() == BillStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot update bill in state: " + existing.getStatus()
            );
        }

        // Allowed updates only
        existing.setConsumerName(request.consumerName());
        existing.setProviderName(request.providerName());
        existing.setServiceNumber(request.serviceNumber());
        existing.setBillingStartDate(request.billingStartDate());
        existing.setBillingEndDate(request.billingEndDate());
        existing.setDueDate(request.dueDate());
        existing.setAmountDue(request.amountDue());
        existing.setCurrency(request.currency());

        return BillMapper.toDetail(
                billRepository.save(existing)
        );
    }

    public void deleteBill(UUID billId) {
        BillEntity bill = getEntityOrThrow(billId);

        if (bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException("Paid bill cannot be deleted");
        }

        BillStateMachine.validateTransition(
                bill.getStatus(),
                BillStatus.CANCELLED
        );

        bill.setStatus(BillStatus.CANCELLED);
        billRepository.save(bill);
    }

    /* =====================================================
     * 3️⃣ STATUS TRANSITIONS
     * ===================================================== */

    public BillDetail markIngested(UUID billId) {
        return transition(billId, BillStatus.INGESTED);
    }

    public BillDetail markVerified(UUID billId) {
        return transition(billId, BillStatus.VERIFIED);
    }

    public void markPaid(UUID billId, UUID paymentId) {
        BillEntity bill = getEntityOrThrow(billId);

        BillStateMachine.validateTransition(
                bill.getStatus(),
                BillStatus.PAID
        );

        bill.setPaymentId(paymentId);
        bill.setStatus(BillStatus.PAID);

        BillMapper.toDetail(
                billRepository.save(bill)
        );
    }

    /* =====================================================
     * 4️⃣ OVERDUE SCHEDULER
     * ===================================================== */

    /**
     * Marks VERIFIED bills as OVERDUE when due date has passed.
     * Intended for scheduler use.
     */
    public void updateOverdue() {
        LocalDate today = LocalDate.now();

        billRepository.findByDueDateBefore(today).stream()
                .filter(b -> b.getStatus() == BillStatus.VERIFIED)
                .forEach(bill -> {
                    BillStateMachine.validateTransition(
                            bill.getStatus(),
                            BillStatus.OVERDUE
                    );
                    bill.setStatus(BillStatus.OVERDUE);
                    log.warn("Bill {} marked OVERDUE", bill.getId());
                });
    }

    /* =====================================================
     * Internal Helpers
     * ===================================================== */

    private BillDetail transition(UUID billId, BillStatus next) {
        BillEntity bill = getEntityOrThrow(billId);

        BillStateMachine.validateTransition(
                bill.getStatus(),
                next
        );

        bill.setStatus(next);
        return BillMapper.toDetail(
                billRepository.save(bill)
        );
    }

    private BillEntity getEntityOrThrow(UUID billId) {
        return billRepository.findById(billId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bill not found: " + billId
                        )
                );
    }
}
