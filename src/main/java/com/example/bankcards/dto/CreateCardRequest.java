package com.example.bankcards.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CreateCardRequest(
        @NotNull @Pattern(regexp = "\\d{16}", message = "card number must contain exactly 16 digits") String cardNumber,
        @NotNull Long ownerId,
        @NotNull @Future LocalDate expirationDate
) {
}
