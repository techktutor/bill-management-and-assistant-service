package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.BillCreateResponse;
import com.wells.bill.assistant.model.BillDetails;
import com.wells.bill.assistant.model.BillSummary;
import com.wells.bill.assistant.model.BillUpdateRequest;
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
    public UUID createBill(BillDetails req) {
        log.info("Creating bill for customerId={}", req.getCustomerId());

        if (req.getCustomerId() == null) {
            throw new IllegalArgumentException("customerId is required");
        }

        if (req.getConsumerName() == null || req.getConsumerName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        BillEntity bill = createDomain(req);

        BillEntity saved = billRepository.save(bill);

        log.info("Bill Created with id= {}", saved.getId());
        return saved.getId();
    }

    /**
     * Business updates only (no ingestion, no payments).
     */
    @Transactional
    public BillCreateResponse updateBill(UUID id, BillUpdateRequest updates) {
        BillEntity bill = getBillById(id);
        if (updates.getName() != null) {
            bill.setConsumerName(updates.getName());
        }
        if (updates.getVendor() != null) {
            bill.setVendor(updates.getVendor());
        }
        if (updates.getCategory() != null) {
            bill.setCategory(updates.getCategory());
        }
        if (updates.getStatus() != null) {
            bill.setStatus(updates.getStatus());
        }

        log.info("Bill updated: id={}", bill.getId());
        return toResponse(bill);
    }

    /**
     * Mark bill PAID – invoked ONLY by PaymentExecutionService.
     */
    @Transactional
    public void markPaid(UUID billId, String paymentId) {
        BillEntity bill = getBillById(billId);

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
    @Transactional
    public void updateOverdue() {
        LocalDate today = LocalDate.now();
        List<BillEntity> overdueBills = billRepository
                .findByStatusAndDueDateBefore(BillStatus.VERIFIED, today);

        overdueBills.forEach(bill -> {
            bill.setStatus(BillStatus.OVERDUE);
            log.warn("Bill {} marked OVERDUE", bill.getId());
        });
    }

    @Transactional(readOnly = true)
    public BillEntity getBillById(UUID id) {
        log.info("Fetching bill entity for id={}", id);
        return billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BillSummary> listByStatus(BillStatus status) {
        log.info("Listing bills with status={}", status.name());
        return billRepository.findByStatus(status)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findAllUnpaid() {
        log.info("Listing all unpaid bills");
        return billRepository
                .findByStatusIn(List.of(BillStatus.VERIFIED, BillStatus.OVERDUE))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findByDueDateRange(LocalDate start, LocalDate end) {
        log.info("Finding bills due between {} and {}", start, end);
        return billRepository
                .findByDueDateBetween(start, end)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findUnpaidByDueDateRange(LocalDate start, LocalDate end) {
        log.info("Finding unpaid bills due between {} and {}", start, end);
        return billRepository
                .findByDueDateBetweenAndStatusIn(
                        start,
                        end,
                        List.of(BillStatus.VERIFIED, BillStatus.OVERDUE)
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillSummary> findByDueDateAfterAndStatusIn(LocalDate start) {
        log.info("Finding unpaid bills due after: {}", start);
        return billRepository
                .findByDueDateAfterAndStatusIn(
                        start,
                        List.of(BillStatus.VERIFIED, BillStatus.OVERDUE)
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public BillSummary getBillSummary(UUID id) {
        log.info("Fetching bill summary for id={}", id);
        return toSummary(getBillById(id));
    }

    private BillCreateResponse toResponse(BillEntity bill) {
        BillCreateResponse response = new BillCreateResponse();
        BeanUtils.copyProperties(bill, response);
        return response;
    }

    private BillSummary toSummary(BillEntity bill) {
        return BillSummary.from(bill);
    }

    private static BillEntity createDomain(BillDetails req) {
        BillEntity bill = new BillEntity();
        bill.setCustomerId(req.getCustomerId());
        bill.setConsumerName(req.getConsumerName());
        bill.setConsumerNumber(req.getConsumerNumber());
        bill.setFileName(req.getFileName());
        bill.setAmount(req.getAmount());
        bill.setCurrency("INR");
        bill.setStatus(BillStatus.UPLOADED);
        bill.setDueDate(req.getDueDate());
        bill.setVendor(req.getVendor());
        bill.setCategory(req.getCategory() == null ? BillCategory.OTHER : req.getCategory());
        bill.setAutoPayEnabled(Boolean.TRUE.equals(req.getAutoPayEnabled()));
        bill.setStatus(BillStatus.UPLOADED);
        return bill;
    }
}
