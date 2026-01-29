package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.model.BillCategory;
import com.wells.bill.assistant.model.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<BillEntity, UUID> {
    List<BillEntity> findByDueDateBetween(LocalDate start, LocalDate end);

    List<BillEntity> findByStatusAndDueDateBefore(
            BillStatus status,
            LocalDate date
    );

    List<BillEntity> findByStatusIn(List<BillStatus> statuses);

    List<BillEntity> findByDueDateBefore(LocalDate date);

    List<BillEntity> findByDueDateAfter(LocalDate date);

    Optional<BillEntity> findByIdAndUserId(
            UUID billId,
            UUID userId
    );

    Page<BillEntity> findByUserId(UUID userId, Pageable pageable);

    List<BillEntity> findByUserIdAndStatusIn(UUID userId, Collection<BillStatus> statuses);

    @Query("""
            SELECT b
            FROM BillEntity b
            WHERE b.userId = :userId
              AND b.status <> com.wells.bill.assistant.model.BillStatus.PAID
              AND b.dueDate < :today
            ORDER BY b.dueDate ASC
            """)
    List<BillEntity> findOverdueBills(UUID userId, LocalDate today);

    List<BillEntity> findByPaymentId(UUID paymentId);

    @Query("""
            SELECT b
            FROM BillEntity b
            WHERE b.userId = :userId
              AND b.status <> com.wells.bill.assistant.model.BillStatus.PAID
              AND b.dueDate BETWEEN :start AND :end
            ORDER BY b.dueDate ASC
            """)
    List<BillEntity> findBillsDueSoon(UUID userId, LocalDate start, LocalDate end);

    @Query("""
                SELECT b
                FROM BillEntity b
                WHERE b.userId = :userId
                  AND LOWER(b.providerName) LIKE LOWER(CONCAT('%', :providerName, '%'))
            """)
    List<BillEntity> findBillsByUserAndProviderName(
            @Param("userId") UUID userId,
            @Param("providerName") String providerName
    );

    @Query("""
            SELECT b.billCategory, SUM(b.amountDue)
            FROM BillEntity b
            WHERE b.userId = :userId
              AND b.billingEndDate BETWEEN :start AND :end
            GROUP BY b.billCategory
            """)
    List<Object[]> getSpendByCategory(UUID userId, LocalDate start, LocalDate end);

    @Query("""
            SELECT COALESCE(SUM(b.amountDue), 0)
            FROM BillEntity b
            WHERE b.userId = :userId
              AND b.status <> com.wells.bill.assistant.model.BillStatus.PAID
              AND b.dueDate < :today
            """)
    BigDecimal getTotalOverdueAmount(UUID userId, LocalDate today);

    List<BillEntity> findByBillCategory(BillCategory category);
}
