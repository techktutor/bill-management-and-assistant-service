package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.entity.PaymentStatus;
import com.wells.bill.assistant.entity.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    // FIXED: paymentId is a String
    Optional<PaymentEntity> findByPaymentId(String paymentId);

    List<PaymentEntity> findByStatusAndScheduledDateLessThanEqual(
            PaymentStatus paymentStatus,
            LocalDate asOfDate
    );

    List<PaymentEntity> findByPaymentTypeAndStatusAndScheduledDateLessThanEqual(
            PaymentType paymentType,
            PaymentStatus paymentStatus,
            LocalDate asOfDate);
}
