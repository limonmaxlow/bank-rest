package com.example.bankcards.controller;

import com.example.bankcards.dto.PageResponse;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "User management (admin only)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users (paged)")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(userService.getAllUsers(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Enable or disable a user")
    public ResponseEntity<UserResponse> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
