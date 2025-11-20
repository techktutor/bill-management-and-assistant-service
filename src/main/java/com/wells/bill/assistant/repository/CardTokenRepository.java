package com.wells.bill.assistant.repository;

import com.wells.bill.assistant.entity.CardToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardTokenRepository extends JpaRepository<CardToken, UUID> {
    Optional<CardToken> findByToken(String token);
    List<CardToken> findByCardId(UUID cardId);
}
