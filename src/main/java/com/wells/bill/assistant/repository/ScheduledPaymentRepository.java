package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.PaymentScheduleStatus;
import com.wells.bill.assistant.entity.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, UUID> {
    List<ScheduledPayment> findByScheduledDateAndStatus(LocalDate date, PaymentScheduleStatus status);
}
