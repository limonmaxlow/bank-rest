package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Transfers between own cards")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Transfer money between the current user's own cards")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transferService.transfer(user.getId(), request));
    }
}
