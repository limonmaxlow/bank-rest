package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.dto.JwtResponse;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean(name = "jwtAuthFilter")
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void register_validRequest_returns201WithToken() throws Exception {
        when(authService.register(any())).thenReturn(new JwtResponse("some-jwt-token"));

        RegisterRequest request = new RegisterRequest("alice", "password123", "alice@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("some-jwt-token"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "password123", "not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "123", "alice@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        when(authService.login(any())).thenReturn(new JwtResponse("login-jwt-token"));

        LoginRequest request = new LoginRequest("alice", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-jwt-token"));
    }
}
