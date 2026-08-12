package com.example.bankcards.service;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.dto.JwtResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public JwtResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }

        Role role = userRepository.count() == 0 ? Role.ROLE_ADMIN : Role.ROLE_USER;

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new JwtResponse(token);
    }

    public JwtResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        String token = jwtService.generateToken(user);
        return new JwtResponse(token);
    }
}
