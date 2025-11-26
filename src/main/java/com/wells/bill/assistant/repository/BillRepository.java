package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.BillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<BillEntity, Long> {
    List<BillEntity> findByDueDateBetween(LocalDate start, LocalDate end);
    List<BillEntity> findByDueDateBetweenAndStatusIn(
            LocalDate start,
            LocalDate end,
            List<String> statusList
    );
}

