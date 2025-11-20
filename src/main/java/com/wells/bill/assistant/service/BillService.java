package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.Bill;
import com.wells.bill.assistant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {
    private final BillRepository billRepository;

    public Bill createBill(Bill bill) {
        return billRepository.save(bill);
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    public Bill getBill(Long id) {
        return billRepository.findById(id).orElseThrow(() -> new RuntimeException("Bill not found: " + id));
    }

    public Bill updateBill(Long id, Bill updated) {
        Bill b = getBill(id);
        b.setName(updated.getName());
        b.setAmount(updated.getAmount());
        b.setDueDate(updated.getDueDate());
        b.setStatus(updated.getStatus());
        return billRepository.save(b);
    }

    public void deleteBill(Long id) {
        billRepository.deleteById(id);
    }
}
