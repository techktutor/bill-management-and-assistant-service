package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.ContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContextRepository extends JpaRepository<ContextEntity, String> {
    Optional<ContextEntity> findTopByUserIdOrderByLastAccessTimeDesc(UUID userId);
}
