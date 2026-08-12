package com.example.bankcards.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long fromCardId,
        Long toCardId,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
