package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled
) {
}
