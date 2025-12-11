package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.model.BillCreateRequest;
import com.wells.bill.assistant.model.BillCreateResponse;
import com.wells.bill.assistant.model.BillUpdateRequest;
import com.wells.bill.assistant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillService {

    private final VectorStore vectorStore;
    private final BillRepository billRepository;

    /**
     * Create a bill from DTO. Validates required fields and maps to entity.
     */
    public BillCreateResponse createBill(BillCreateRequest req) {
        if (req.getCustomerId() == null) throw new IllegalArgumentException("customerId is required");
        if (req.getDueDate() == null) throw new IllegalArgumentException("dueDate is required");
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("amount must be > 0");

        BillEntity billEntity = getBillEntity(req);

        BillEntity saved = billRepository.save(billEntity);
        log.info("Bill created: id={} name={} due={}", saved.getId(), saved.getName(), saved.getDueDate());
        // ---- STORE TO VECTOR STORE ----
        Document doc = toVectorDocument(saved);
        vectorStore.add(List.of(doc));
        log.info("Stored bill {} into vector DB", saved.getId());
        return toResponse(saved);
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

    public BillCreateResponse getBill(UUID id) {
        BillEntity billEntity = getBillEntity(id);
        return toResponse(billEntity);
    }

    private BillEntity getBillEntity(UUID id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id));
    }

    public List<BillCreateResponse> listAllBills() {
        return billRepository.findAll()
                .stream()
                .map(billEntity -> {
                    BillCreateResponse response = new BillCreateResponse();
                    BeanUtils.copyProperties(billEntity, response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<BillCreateResponse> listByStatus(BillStatus status) {
        return billRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BillCreateResponse updateBill(UUID id, BillUpdateRequest updates) {
        BillEntity billEntity = getBillEntity(id);
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
        // ---- UPDATE VECTOR STORE ----
        Document doc = toVectorDocument(saved);
        vectorStore.add(List.of(doc)); // overwrites existing by ID
        log.info("Updated bill {} in vector DB", saved.getId());
        BillCreateResponse response = new BillCreateResponse();
        BeanUtils.copyProperties(saved, response);
        return response;
    }

    public void deleteBill(UUID id) {
        billRepository.deleteById(id);
        log.info("Bill deleted: {}", id);
    }

    /**
     * Mark a bill as paid and optionally attach the paymentId reference.
     */
    @Transactional
    public void markAsPaid(UUID billId, String paymentId, boolean systemAction) {
        BillEntity bill = getBillEntity(billId);

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
        LocalDate today = LocalDate.now();

        // Fetch only those bills that need to be updated
        List<BillEntity> overdueBills = billRepository.findByStatusAndDueDateBefore(BillStatus.PENDING, today);

        overdueBills.forEach(bill -> {
            bill.setStatus(BillStatus.OVERDUE);
            log.warn("Bill {} marked OVERDUE", bill.getId());
        });

        // Save all at once (better performance than multiple save calls)
        billRepository.saveAll(overdueBills);
    }

    public List<BillCreateResponse> findByDueDateRange(LocalDate start, LocalDate end) {
        return billRepository.findByDueDateBetween(start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BillCreateResponse> findUpcomingUnpaidBills(LocalDate start, LocalDate end) {
        return billRepository.findByDueDateBetweenAndStatusIn(start, end, List.of(BillStatus.PENDING, BillStatus.OVERDUE)).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BillCreateResponse> findUnpaidByDueDateRange(LocalDate start, LocalDate end) {
        return findUpcomingUnpaidBills(start, end);
    }

    public List<BillCreateResponse> findAllUnpaid() {
        return billRepository.findByStatusIn(List.of(BillStatus.PENDING, BillStatus.OVERDUE)).stream()
                .map(this::toResponse)
                .toList();
    }

    private BillCreateResponse toResponse(BillEntity bill) {
        BillCreateResponse response = new BillCreateResponse();
        BeanUtils.copyProperties(bill, response);
        return response;
    }

    private Document toVectorDocument(BillEntity bill) {
        String text = bill.getExtractedText();
        if (text == null || text.isBlank()) {
            text = bill.getName() + " " + bill.getVendor() + " " + bill.getAmount();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bill_id", bill.getId());
        metadata.put("vendor", bill.getVendor());
        metadata.put("amount", bill.getAmount());
        metadata.put("parent_document_id", bill.getId().toString());

        return Document.builder()
                .id(String.valueOf(bill.getId()))
                .text(text)
                .metadata(metadata)
                .build();
    }

}
