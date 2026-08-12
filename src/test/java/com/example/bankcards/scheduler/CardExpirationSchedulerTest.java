package com.example.bankcards.scheduler;

import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardExpirationSchedulerTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private CardExpirationScheduler scheduler;

    @Test
    void expireOverdueCards_delegatesToCardService() {
        when(cardService.expireOverdueCards()).thenReturn(3);

        scheduler.expireOverdueCards();

        verify(cardService, times(1)).expireOverdueCards();
    }
}
