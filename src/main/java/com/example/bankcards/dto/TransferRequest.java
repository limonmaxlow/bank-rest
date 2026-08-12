package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long fromCardId,
        @NotNull Long toCardId,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount
) {
}
