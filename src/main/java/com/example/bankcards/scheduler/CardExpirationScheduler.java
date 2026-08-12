package com.example.bankcards.scheduler;

import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CardExpirationScheduler.class);

    private final CardService cardService;

    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueCards() {
        int count = cardService.expireOverdueCards();
        log.info("Card expiration job marked {} card(s) as EXPIRED", count);
    }
}
