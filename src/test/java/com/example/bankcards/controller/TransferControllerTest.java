package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    private User regularUser() {
        return User.builder().id(2L).username("alice").role(Role.ROLE_USER).enabled(true).build();
    }

    private RequestPostProcessor asUser(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void transfer_success_returns200() throws Exception {
        TransferResponse response = new TransferResponse(1L, 10L, 20L, new BigDecimal("30.00"), LocalDateTime.now());
        when(transferService.transfer(anyLong(), any())).thenReturn(response);

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("30.00"));

        mockMvc.perform(post("/api/transfers")
                        .with(asUser(regularUser()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(30.00));
    }

    @Test
    void transfer_insufficientFunds_returns422() throws Exception {
        when(transferService.transfer(anyLong(), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds on source card"));

        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("500.00"));

        mockMvc.perform(post("/api/transfers")
                        .with(asUser(regularUser()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transfer_negativeAmount_returns400() throws Exception {
        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/transfers")
                        .with(asUser(regularUser()))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
