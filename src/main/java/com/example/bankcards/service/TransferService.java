package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public TransferResponse transfer(Long requesterId, TransferRequest request) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new BadRequestException("Cannot transfer to the same card");
        }

        Long firstId = Math.min(request.fromCardId(), request.toCardId());
        Long secondId = Math.max(request.fromCardId(), request.toCardId());

        Card first = lockCard(firstId);
        Card second = lockCard(secondId);

        Card fromCard = first.getId().equals(request.fromCardId()) ? first : second;
        Card toCard = first.getId().equals(request.toCardId()) ? first : second;

        if (!fromCard.getOwner().getId().equals(requesterId) || !toCard.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("You can only transfer between your own cards");
        }

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new BadRequestException("Source card is not active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new BadRequestException("Destination card is not active");
        }
        if (fromCard.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Source card has expired");
        }
        if (toCard.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Destination card has expired");
        }

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
        toCard.setBalance(toCard.getBalance().add(request.amount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transfer transfer = Transfer.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.amount())
                .createdAt(LocalDateTime.now())
                .build();
        transfer = transferRepository.save(transfer);

        return toResponse(transfer);
    }

    private Card lockCard(Long id) {
        return cardRepository.findWithLockById(id)
                .orElseThrow(() -> new NotFoundException("Card not found: " + id));
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromCard().getId(),
                transfer.getToCard().getId(),
                transfer.getAmount(),
                transfer.getCreatedAt()
        );
    }
}
