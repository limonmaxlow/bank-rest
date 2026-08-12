package com.example.bankcards.service;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private CardEncryptionUtil encryptionUtil;

    @InjectMocks
    private CardService cardService;

    private User owner;
    private Card card;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("alice").role(Role.ROLE_USER).enabled(true).build();
        card = Card.builder()
                .id(5L)
                .owner(owner)
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .lastFourDigits("4321")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();
    }

    @Test
    void createCard_success_masksNumberAndEncrypts() {
        when(userService.getUserEntity(1L)).thenReturn(owner);
        when(encryptionUtil.encrypt(any())).thenReturn("encrypted-value");
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CreateCardRequest request = new CreateCardRequest("1234567812345678", 1L, LocalDate.now().plusYears(3));
        CardResponse response = cardService.createCard(request);

        assertThat(response.maskedCardNumber()).isEqualTo("**** **** **** 5678");
        assertThat(response.ownerUsername()).isEqualTo("alice");
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCard_wrongOwnerNotAdmin_throwsForbidden() {
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.getCard(5L, 999L, false))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void getCard_admin_canAccessAnyCard() {
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));

        CardResponse response = cardService.getCard(5L, 999L, true);

        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void getCard_notFound_throwsNotFoundException() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(999L, 1L, false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void activateCard_expiredCard_throwsBadRequest() {
        card.setExpirationDate(LocalDate.now().minusDays(1));
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.activateCard(5L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requestBlock_ownCard_setsBlockedStatus() {
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse response = cardService.requestBlock(5L, 1L);

        assertThat(response.status()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void requestBlock_notOwnCard_throwsForbidden() {
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.requestBlock(5L, 2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void getCard_activeButPastExpirationDate_reportsExpiredStatus() {
        card.setExpirationDate(LocalDate.now().minusDays(1));
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));

        CardResponse response = cardService.getCard(5L, 1L, false);

        assertThat(response.status()).isEqualTo(CardStatus.EXPIRED);
    }

    @Test
    void getCard_blockedAndPastExpirationDate_staysBlockedNotExpired() {
        card.setStatus(CardStatus.BLOCKED);
        card.setExpirationDate(LocalDate.now().minusDays(1));
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));

        CardResponse response = cardService.getCard(5L, 1L, false);

        assertThat(response.status()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void expireOverdueCards_movesOverdueActiveCardsToExpired() {
        Card overdue = Card.builder()
                .id(7L)
                .owner(owner)
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .lastFourDigits("9999")
                .expirationDate(LocalDate.now().minusDays(5))
                .build();

        when(cardRepository.findByStatusAndExpirationDateBefore(CardStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(overdue));
        when(cardRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int updated = cardService.expireOverdueCards();

        assertThat(updated).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(CardStatus.EXPIRED);
    }
}
