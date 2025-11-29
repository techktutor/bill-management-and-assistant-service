package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.model.PaymentScheduleStatus;
import com.wells.bill.assistant.entity.ScheduledPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPaymentEntity, UUID> {
    List<ScheduledPaymentEntity> findAllByStatusAndScheduledDateLessThanEqual(PaymentScheduleStatus paymentScheduleStatus, LocalDate asOfDate);
}
