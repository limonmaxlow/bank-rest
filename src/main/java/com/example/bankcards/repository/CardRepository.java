package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    List<Card> findByStatusAndExpirationDateBefore(CardStatus status, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id")
    Optional<Card> findWithLockById(@Param("id") Long id);
}
