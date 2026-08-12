package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CardRepositorySpecificationTest {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findAll_combinesSearchAndStatusFiltersSimultaneously() {
        User owner = userRepository.save(User.builder()
                .username("carol")
                .password("hash")
                .email("carol@test.com")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build());

        Card activeMatchingSearch = cardRepository.save(Card.builder()
                .cardNumberEncrypted("enc-1")
                .lastFourDigits("1234")
                .owner(owner)
                .expirationDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build());

        cardRepository.save(Card.builder()
                .cardNumberEncrypted("enc-2")
                .lastFourDigits("1234")
                .owner(owner)
                .expirationDate(LocalDate.now().plusYears(1))
                .status(CardStatus.BLOCKED)
                .balance(BigDecimal.ZERO)
                .build());

        cardRepository.save(Card.builder()
                .cardNumberEncrypted("enc-3")
                .lastFourDigits("5678")
                .owner(owner)
                .expirationDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build());

        Specification<Card> spec = (root, query, cb) -> cb.equal(root.get("owner").get("id"), owner.getId());
        spec = spec.and((root, query, cb) -> cb.like(root.get("lastFourDigits"), "%1234%"));
        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), CardStatus.ACTIVE));

        var result = cardRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(activeMatchingSearch.getId());
    }
}
