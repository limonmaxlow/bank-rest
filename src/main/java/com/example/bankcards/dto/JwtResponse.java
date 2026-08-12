package com.example.bankcards.dto;

public record JwtResponse(String token, String tokenType) {
    public JwtResponse(String token) {
        this(token, "Bearer");
    }
}
