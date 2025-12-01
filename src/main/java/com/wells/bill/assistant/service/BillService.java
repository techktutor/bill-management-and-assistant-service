package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.BillCreateRequest;
import com.wells.bill.assistant.model.BillUpdateRequest;
import com.wells.bill.assistant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;

    /**
     * Create a bill from DTO. Validates required fields and maps to entity.
     */
    public BillEntity createBill(BillCreateRequest req) {
        if (req.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");
        if (req.getDueDate() == null) throw new IllegalArgumentException("dueDate is required");
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be > 0");

        BillEntity billEntity = getBillEntity(req);

        BillEntity saved = billRepository.save(billEntity);
        log.info("Bill created: id={} name={} due={}", saved.getId(), saved.getName(), saved.getDueDate());
        return saved;
    }

    private static BillEntity getBillEntity(BillCreateRequest req) {
        BillEntity billEntity = new BillEntity();
        billEntity.setCustomerId(req.getCustomerId());
        billEntity.setName(req.getName());
        billEntity.setAmount(req.getAmount());
        billEntity.setDueDate(req.getDueDate());
        billEntity.setVendor(req.getVendor());
        billEntity.setCategory(req.getCategory() == null ? BillCategory.OTHER : req.getCategory());
        billEntity.setPeriodStart(req.getPeriodStart());
        billEntity.setPeriodEnd(req.getPeriodEnd());
        billEntity.setLateFee(req.getLateFee());
        billEntity.setDocumentUrl(req.getDocumentUrl());
        billEntity.setExtractedText(req.getExtractedText());
        billEntity.setSource(req.getSource());
        billEntity.setAutoPayEnabled(req.getAutoPayEnabled() != null ? req.getAutoPayEnabled() : Boolean.FALSE);
        billEntity.setAutoPayScheduledDate(req.getAutoPayScheduledDate());
        return billEntity;
    }

    public BillEntity getBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id));
    }

    public List<BillEntity> listAllBills() {
        return billRepository.findAll();
    }

    public List<BillEntity> listByStatus(BillStatus status) {
        return billRepository.findAll().stream()
                .filter(b -> b.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Transactional
    public BillEntity updateBill(Long id, BillUpdateRequest updates) {
        BillEntity billEntity = getBill(id);
        if (updates.getName() != null) billEntity.setName(updates.getName());
        if (updates.getVendor() != null) billEntity.setVendor(updates.getVendor());
        if (updates.getCategory() != null) billEntity.setCategory(updates.getCategory());
        if (updates.getPeriodStart() != null) billEntity.setPeriodStart(updates.getPeriodStart());
        if (updates.getPeriodEnd() != null) billEntity.setPeriodEnd(updates.getPeriodEnd());
        if (updates.getLateFee() != null) billEntity.setLateFee(updates.getLateFee());
        if (updates.getDocumentUrl() != null) billEntity.setDocumentUrl(updates.getDocumentUrl());
        if (updates.getExtractedText() != null) billEntity.setExtractedText(updates.getExtractedText());
        if (updates.getSource() != null) billEntity.setSource(updates.getSource());
        BillEntity saved = billRepository.save(billEntity);
        log.info("Bill updated: id={}", saved.getId());
        return saved;
    }

    public void deleteBill(Long id) {
        billRepository.deleteById(id);
        log.info("Bill deleted: {}", id);
    }

    /**
     * Mark a bill as paid and optionally attach the paymentId reference.
     */
    @Transactional
    public void markAsPaid(Long billId, String paymentId, boolean systemAction) {
        BillEntity bill = getBill(billId);

        if (bill.getStatus() == BillStatus.PAID) {
            log.warn("Attempt to mark an already PAID bill. billId={}", billId);
            return; // idempotent
        }

        // --- SYSTEM ACTION: paymentId REQUIRED ---
        if (systemAction) {
            if (paymentId == null || paymentId.isBlank()) {
                throw new IllegalStateException("System marking bill as PAID requires a paymentId");
            }
        }

        bill.setStatus(BillStatus.PAID);
        bill.setPaymentId(paymentId);

        billRepository.save(bill);

        if (systemAction) {
            log.info("Bill marked PAID by system. billId={} paymentId={}", billId, paymentId);
        } else {
            log.warn("Bill manually overridden to PAID. billId={} paymentId={}", billId, paymentId);
            // TODO: write audit entry
        }
    }

    /**
     * Mark overdue bills (PENDING -> OVERDUE) based on today's date.
     */
    @Transactional
    public void updateOverdue() {
        List<BillEntity> bills = billRepository.findAll();
        LocalDate today = LocalDate.now();
        bills.stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING && b.getDueDate() != null && b.getDueDate().isBefore(today))
                .forEach(b -> {
                    b.setStatus(BillStatus.OVERDUE);
                    billRepository.save(b);
                    log.warn("Bill {} marked OVERDUE", b.getId());
                });
    }

    public List<BillEntity> findByDueDateRange(LocalDate start, LocalDate end) {
        return billRepository.findAll().stream()
                .filter(b -> b.getDueDate() != null && !b.getDueDate().isBefore(start) && !b.getDueDate().isAfter(end))
                .collect(Collectors.toList());
    }

    public List<BillEntity> findUpcomingUnpaidBills(LocalDate start, LocalDate end) {
        return billRepository.findAll().stream()
                .filter(b -> b.getDueDate() != null && !b.getDueDate().isBefore(start) && !b.getDueDate().isAfter(end))
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.OVERDUE)
                .collect(Collectors.toList());
    }

    public List<BillEntity> findUnpaidByDueDateRange(LocalDate start, LocalDate end) {
        return findUpcomingUnpaidBills(start, end);
    }

    public List<BillEntity> findAllUnpaid() {
        return billRepository.findAll().stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.OVERDUE)
                .collect(Collectors.toList());
    }
}
