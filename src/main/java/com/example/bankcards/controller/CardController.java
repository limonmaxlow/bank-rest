package com.example.bankcards.controller;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.PageResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new card for a user (admin only)")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all cards in the system (admin only)")
    public ResponseEntity<PageResponse<CardResponse>> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(cardService.getAllCards(pageable)));
    }

    @GetMapping
    @Operation(summary = "List the current user's own cards (search + pagination)")
    public ResponseEntity<PageResponse<CardResponse>> getOwnCards(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CardStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(cardService.getOwnCards(user.getId(), search, status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single card by id (owner or admin)")
    public ResponseEntity<CardResponse> getCard(@PathVariable Long id, @AuthenticationPrincipal User user) {
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        return ResponseEntity.ok(cardService.getCard(id, user.getId(), isAdmin));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block a card (admin only)")
    public ResponseEntity<CardResponse> blockCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.blockCard(id));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a card (admin only)")
    public ResponseEntity<CardResponse> activateCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.activateCard(id));
    }

    @PatchMapping("/{id}/request-block")
    @Operation(summary = "Request blocking of own card")
    public ResponseEntity<CardResponse> requestBlock(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cardService.requestBlock(id, user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a card (admin only)")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
