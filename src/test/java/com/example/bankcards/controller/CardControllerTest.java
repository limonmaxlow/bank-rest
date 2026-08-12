package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CardController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private User adminUser() {
        return User.builder().id(1L).username("admin").role(Role.ROLE_ADMIN).enabled(true).build();
    }

    private User regularUser() {
        return User.builder().id(2L).username("alice").role(Role.ROLE_USER).enabled(true).build();
    }

    private RequestPostProcessor asUser(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void createCard_asAdmin_returns201() throws Exception {
        CardResponse response = new CardResponse(1L, "**** **** **** 5678", "alice",
                LocalDate.now().plusYears(2), CardStatus.ACTIVE, BigDecimal.ZERO);
        when(cardService.createCard(any())).thenReturn(response);

        CreateCardRequest request = new CreateCardRequest("1234567812345678", 2L, LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/cards")
                        .with(asUser(adminUser()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createCard_asRegularUser_returns403() throws Exception {
        CreateCardRequest request = new CreateCardRequest("1234567812345678", 2L, LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/cards")
                        .with(asUser(regularUser()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOwnCards_asUser_returns200() throws Exception {
        Page<CardResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(cardService.getOwnCards(anyLong(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/cards").with(asUser(regularUser())))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCards_asRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/cards/admin/all").with(asUser(regularUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCards_asAdmin_returns200() throws Exception {
        Page<CardResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(cardService.getAllCards(any())).thenReturn(page);

        mockMvc.perform(get("/api/cards/admin/all").with(asUser(adminUser())))
                .andExpect(status().isOk());
    }
}
