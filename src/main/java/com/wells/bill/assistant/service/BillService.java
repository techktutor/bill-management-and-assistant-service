package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BillService {

    private final BillRepository billRepository;

    public BillEntity createBill(BillEntity bill) {
        if (bill.getDueDate() == null || bill.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid bill amount or due date");
        }

        bill.setStatus("PENDING");
        BillEntity saved = billRepository.save(bill);
        log.info("Bill created: id={} name={} due={}", saved.getId(), saved.getName(), saved.getDueDate());
        return saved;
    }

    public BillEntity getBill(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id));
    }

    public List<BillEntity> listAllBills() {
        return billRepository.findAll();
    }

    public List<BillEntity> listByStatus(String status) {
        return billRepository.findAll()
                .stream()
                .filter(b -> status.equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    public BillEntity updateBill(Long id, BillEntity updates) {
        BillEntity b = getBill(id);
        b.setName(updates.getName());
        b.setAmount(updates.getAmount());
        b.setDueDate(updates.getDueDate());
        return billRepository.save(b);
    }

    public void deleteBill(Long id) {
        billRepository.deleteById(id);
        log.info("Bill deleted: {}", id);
    }

    public void markAsPaid(Long billId) {
        BillEntity bill = getBill(billId);
        bill.setStatus("PAID");
        billRepository.save(bill);
        log.info("Bill marked as PAID: {}", billId);
    }

    public void updateOverdues() {
        List<BillEntity> bills = billRepository.findAll();
        LocalDate today = LocalDate.now();
        bills.stream()
                .filter(b -> "PENDING".equals(b.getStatus()) && b.getDueDate().isBefore(today))
                .forEach(b -> {
                    b.setStatus("OVERDUE");
                    billRepository.save(b);
                    log.warn("Bill {} marked OVERDUE", b.getId());
                });
    }

    public List<BillEntity> findByDueDateRange(LocalDate start, LocalDate end) {
        return billRepository.findByDueDateBetween(start, end);
    }

    public List<BillEntity> findUpcomingUnpaidBills(LocalDate start, LocalDate end) {
        return billRepository.findByDueDateBetweenAndStatusIn(
                start,
                end,
                List.of("PENDING", "OVERDUE")
        );
    }
}
