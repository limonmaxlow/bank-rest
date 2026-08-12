package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferService transferService;

    private User owner;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("alice").role(Role.ROLE_USER).enabled(true).build();

        fromCard = Card.builder()
                .id(10L)
                .owner(owner)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .expirationDate(LocalDate.now().plusYears(1))
                .lastFourDigits("1234")
                .build();

        toCard = Card.builder()
                .id(20L)
                .owner(owner)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("50.00"))
                .expirationDate(LocalDate.now().plusYears(1))
                .lastFourDigits("5678")
                .build();
    }

    @Test
    void transfer_success_movesBalanceBetweenOwnCards() {
        when(cardRepository.findWithLockById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findWithLockById(20L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRepository.save(any())).thenAnswer(inv -> {
            var t = inv.getArgument(0, com.example.bankcards.entity.Transfer.class);
            t.setId(99L);
            return t;
        });

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("30.00"));
        TransferResponse response = transferService.transfer(1L, request);

        assertThat(response.amount()).isEqualByComparingTo("30.00");
        assertThat(fromCard.getBalance()).isEqualByComparingTo("70.00");
        assertThat(toCard.getBalance()).isEqualByComparingTo("80.00");
    }

    @Test
    void transfer_insufficientFunds_throwsException() {
        when(cardRepository.findWithLockById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findWithLockById(20L)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("500.00"));

        assertThatThrownBy(() -> transferService.transfer(1L, request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transfer_notOwner_throwsForbidden() {
        User stranger = User.builder().id(2L).username("bob").role(Role.ROLE_USER).enabled(true).build();
        toCard.setOwner(stranger);

        when(cardRepository.findWithLockById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findWithLockById(20L)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transferService.transfer(1L, request))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void transfer_sameCard_throwsBadRequest() {
        TransferRequest request = new TransferRequest(10L, 10L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transferService.transfer(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_blockedSourceCard_throwsBadRequest() {
        fromCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findWithLockById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findWithLockById(20L)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transferService.transfer(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transfer_expiredSourceCardStillActiveStatus_throwsBadRequest() {
        fromCard.setExpirationDate(LocalDate.now().minusDays(1));
        when(cardRepository.findWithLockById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findWithLockById(20L)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transferService.transfer(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void transfer_expiredDestinationCardStillActiveStatus_throwsBadRequest() {
        toCard.setExpirationDate(LocalDate.now().minusDays(1));
        when(cardRepository.findWithLockById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findWithLockById(20L)).thenReturn(Optional.of(toCard));

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> transferService.transfer(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }
}
