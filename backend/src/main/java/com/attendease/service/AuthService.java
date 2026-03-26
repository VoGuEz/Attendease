package com.attendease.service;

import com.attendease.dto.AuthResponse;
import com.attendease.dto.LoginRequest;
import com.attendease.dto.RegisterRequest;
import com.attendease.entity.User;
import com.attendease.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User.Role role;
        try {
            role = (request.getRole() != null && !request.getRole().isBlank())
                ? User.Role.valueOf(request.getRole().toUpperCase())
                : User.Role.STUDENT;
        } catch (IllegalArgumentException e) {
            role = User.Role.STUDENT;
        }

        User user = User.builder()
            .email(request.getEmail())
            .fullName(request.getFullName())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .build();

        user = userService.save(user);
        String token = generateTokenForUser(user);

        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = generateTokenForUser(user);
        return buildAuthResponse(token, user);
    }

    private String generateTokenForUser(User user) {
        org.springframework.security.core.userdetails.User userDetails =
            new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        return jwtTokenProvider.generateToken(userDetails);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
            .token(token)
            .user(AuthResponse.UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build())
            .build();
    }
}
