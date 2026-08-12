package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardResponse(
        Long id,
        String maskedCardNumber,
        String ownerUsername,
        LocalDate expirationDate,
        CardStatus status,
        BigDecimal balance
) {
}
