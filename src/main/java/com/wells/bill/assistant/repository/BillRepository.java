package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<BillEntity, UUID> {
    List<BillEntity> findByDueDateBetween(LocalDate start, LocalDate end);

    List<BillEntity> findByDueDateBetweenAndStatusIn(
            LocalDate start,
            LocalDate end,
            List<BillStatus> statusList
    );

    List<BillEntity> findByDueDateAfterAndStatusIn(
            LocalDate start,
            List<BillStatus> statusList
    );

    List<BillEntity> findByCustomerId(UUID customerId);

    List<BillEntity> findByCustomerIdAndStatusIn(
            UUID customerId,
            List<BillStatus> statuses
    );

    List<BillEntity> findByStatusIn(List<BillStatus> statuses);

    List<BillEntity> findByCustomerIdAndDueDateBetween(
            UUID customerId,
            LocalDate start,
            LocalDate end
    );

    List<BillEntity> findByStatus(BillStatus status);

    List<BillEntity> findByStatusAndDueDateBefore(BillStatus status, LocalDate date);
    List<BillEntity> findByStatusAndDueDateAfter(BillStatus status, LocalDate date);
}
