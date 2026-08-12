package com.example.bankcards.service;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import com.example.bankcards.util.CardMaskUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;
    private final CardEncryptionUtil encryptionUtil;

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User owner = userService.getUserEntity(request.ownerId());

        String lastFour = request.cardNumber().substring(request.cardNumber().length() - 4);
        String encrypted = encryptionUtil.encrypt(request.cardNumber());

        Card card = Card.builder()
                .cardNumberEncrypted(encrypted)
                .lastFourDigits(lastFour)
                .owner(owner)
                .expirationDate(request.expirationDate())
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        return toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getOwnCards(Long ownerId, String search, CardStatus status, Pageable pageable) {
        Specification<Card> spec = ownedBy(ownerId);

        if (search != null && !search.isBlank()) {
            spec = spec.and(lastFourDigitsContains(search));
        }
        if (status != null) {
            spec = spec.and(hasStatus(status));
        }

        return cardRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private Specification<Card> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    private Specification<Card> lastFourDigitsContains(String search) {
        return (root, query, cb) -> cb.like(root.get("lastFourDigits"), "%" + search + "%");
    }

    private Specification<Card> hasStatus(CardStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(Long id, Long requesterId, boolean isAdmin) {
        Card card = getCardEntity(id);
        assertOwnershipOrAdmin(card, requesterId, isAdmin);
        return toResponse(card);
    }

    @Transactional
    public CardResponse blockCard(Long id) {
        Card card = getCardEntity(id);
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse activateCard(Long id) {
        Card card = getCardEntity(id);
        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot activate an expired card");
        }
        card.setStatus(CardStatus.ACTIVE);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse requestBlock(Long id, Long requesterId) {
        Card card = getCardEntity(id);
        if (!card.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("You can only request blocking of your own cards");
        }
        card.setStatus(CardStatus.BLOCKED);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        if (!cardRepository.existsById(id)) {
            throw new NotFoundException("Card not found: " + id);
        }
        cardRepository.deleteById(id);
    }

    @Transactional
    public int expireOverdueCards() {
        var overdueCards = cardRepository.findByStatusAndExpirationDateBefore(CardStatus.ACTIVE, LocalDate.now());
        overdueCards.forEach(card -> card.setStatus(CardStatus.EXPIRED));
        cardRepository.saveAll(overdueCards);
        return overdueCards.size();
    }

    public Card getCardEntity(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Card not found: " + id));
    }

    public void assertOwnershipOrAdmin(Card card, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !card.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("You do not have access to this card");
        }
    }

    public CardResponse toResponse(Card card) {
        CardStatus effectiveStatus = computeEffectiveStatus(card);
        return new CardResponse(
                card.getId(),
                CardMaskUtil.mask(card.getLastFourDigits()),
                card.getOwner().getUsername(),
                card.getExpirationDate(),
                effectiveStatus,
                card.getBalance()
        );
    }

    public CardStatus computeEffectiveStatus(Card card) {
        if (card.getStatus() == CardStatus.ACTIVE && card.getExpirationDate().isBefore(LocalDate.now())) {
            return CardStatus.EXPIRED;
        }
        return card.getStatus();
    }
}
