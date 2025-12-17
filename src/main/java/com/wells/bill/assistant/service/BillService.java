package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.*;
import com.wells.bill.assistant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;

    /**
     * Create a bill in UPLOADED state.
     * No vector ingestion happens here.
     */
    @Transactional
    public BillCreateResponse createBill(BillCreateRequest req) {
        if (req.getCustomerId() == null) {
            throw new IllegalArgumentException("customerId is required");
        }

        if (req.getConsumerName() == null || req.getConsumerName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        BillEntity bill = getBillEntity(req);

        BillEntity saved = billRepository.save(bill);

        log.info("Bill created: id={} name={}", saved.getId(), saved.getConsumerName());
        return toResponse(saved);
    }

    private static BillEntity getBillEntity(BillCreateRequest req) {
        BillEntity bill = new BillEntity();
        bill.setCustomerId(req.getCustomerId());
        bill.setConsumerName(req.getConsumerName());
        bill.setConsumerNumber(req.getConsumerNumber());
        bill.setFileName(req.getFileName());
        bill.setAmount(req.getAmount());
        bill.setCurrency(req.getCurrency());
        bill.setStatus(req.getStatus());
        bill.setDueDate(req.getDueDate());
        bill.setVendor(req.getVendor());
        bill.setCategory(req.getCategory() == null ? BillCategory.OTHER : req.getCategory());
        bill.setAutoPayEnabled(Boolean.TRUE.equals(req.getAutoPayEnabled()));
        bill.setStatus(BillStatus.UPLOADED);
        return bill;
    }

    public UUID createBill(BillDetails billDetails) {
        BillCreateRequest req = new BillCreateRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setConsumerName(billDetails.getConsumerName());
        req.setConsumerNumber(billDetails.getConsumerNumber());
        req.setFileName(billDetails.getFileName());
        req.setAmount(billDetails.getAmount());
        req.setCurrency("INR");
        req.setDueDate(billDetails.getDueDate());
        return createBill(req).getId();
    }

    /**
     * Business updates only (no ingestion, no payments).
     */
    @Transactional
    public BillCreateResponse updateBill(UUID id, BillUpdateRequest updates) {
        BillEntity bill = getBillEntity(id);
        if (updates.getName() != null) {
            bill.setConsumerName(updates.getName());
        }
        if (updates.getVendor() != null) {
            bill.setVendor(updates.getVendor());
        }
        if (updates.getCategory() != null) {
            bill.setCategory(updates.getCategory());
        }

        log.info("Bill updated: id={}", bill.getId());
        return toResponse(bill);
    }

    /**
     * Mark bill PAID – invoked ONLY by PaymentExecutionService.
     */
    public void markPaid(UUID billId, String paymentId) {
        BillEntity bill = getBillEntity(billId);

        if (bill.getStatus() == BillStatus.PAID) {
            return;
        }

        bill.setStatus(BillStatus.PAID);
        bill.setLastSuccessfulPaymentId(paymentId);

        log.info("Bill {} marked PAID via payment {}", billId, paymentId);
    }

    /**
     * Scheduled overdue update.
     */
    public void updateOverdue() {
        LocalDate today = LocalDate.now();
        List<BillEntity> overdueBills = billRepository.findByStatusAndDueDateBefore(BillStatus.PENDING, today);

        overdueBills.forEach(bill -> {
            bill.setStatus(BillStatus.OVERDUE);
            log.warn("Bill {} marked OVERDUE", bill.getId());
        });
    }

    @Transactional(readOnly = true)
    public BillSummary getBill(UUID id) {
        return toSummary(getBillEntity(id));
    }

    @Transactional(readOnly = true)
    public BillEntity getBillEntity(UUID id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BillSummary> listByStatus(BillStatus status) {
        return billRepository.findByStatus(status)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findAllUnpaid() {
        return billRepository
                .findByStatusIn(List.of(BillStatus.PENDING, BillStatus.OVERDUE))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findByDueDateRange(LocalDate start, LocalDate end) {
        return billRepository
                .findByDueDateBetween(start, end)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findUnpaidByDueDateRange(LocalDate start, LocalDate end) {
        return billRepository
                .findByDueDateBetweenAndStatusIn(
                        start,
                        end,
                        List.of(BillStatus.PENDING, BillStatus.OVERDUE)
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findUpcomingUnpaidBills(LocalDate start, LocalDate end) {
        return billRepository
                .findByDueDateBetweenAndStatusIn(
                        start,
                        end,
                        List.of(BillStatus.PENDING, BillStatus.OVERDUE)
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private BillCreateResponse toResponse(BillEntity bill) {
        BillCreateResponse response = new BillCreateResponse();
        BeanUtils.copyProperties(bill, response);
        return response;
    }

    private BillSummary toSummary(BillEntity bill) {
        return BillSummary.from(bill);
    }
}
