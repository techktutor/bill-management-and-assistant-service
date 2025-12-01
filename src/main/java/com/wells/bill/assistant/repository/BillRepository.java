package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<BillEntity, Long> {
    List<BillEntity> findByDueDateBetween(LocalDate start, LocalDate end);

    List<BillEntity> findByDueDateBetweenAndStatusIn(
            LocalDate start,
            LocalDate end,
            List<String> statusList
    );

    List<BillEntity> findByCustomerId(UUID customerId);

    List<BillEntity> findByCustomerIdAndStatus(UUID customerId, BillStatus status);

    List<BillEntity> findByCustomerIdAndStatusIn(UUID customerId, List<BillStatus> statuses);

    List<BillEntity> findByCustomerIdAndDueDateBetween(UUID customerId, LocalDate start, LocalDate end);

    List<BillEntity> findByVendorIgnoreCase(String vendor);
}
