package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.Payment;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

    public void createReserveEntry(Payment payment) {
// create ledger row for reserve
    }

    public void createCaptureEntry(Payment payment) {
// create ledger row for capture
    }
}