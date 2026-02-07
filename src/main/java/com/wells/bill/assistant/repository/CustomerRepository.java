package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
    CustomerEntity findByUserId(UUID userId);
}
