package com.wells.bill.assistant.scheduler;

import com.wells.bill.assistant.service.BillService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BillOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillOverdueScheduler.class);

    private final BillService billService;

    // Run daily at 01:00 AM
    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdue() {
        log.info("Running BillOverdueScheduler at {}", LocalDate.now());
        billService.updateOverdue();
    }
}
