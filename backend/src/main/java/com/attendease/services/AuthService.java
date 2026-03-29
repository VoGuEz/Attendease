package com.attendease.services;

import com.attendease.models.User;
import com.attendease.repositories.UserRepository;
import com.attendease.utils.PasswordUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, EmailService emailService, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.tokenProvider = tokenProvider;
    }

    // signature: 4 strings
    public Map<String, Object> register(String email, String password, String fullName, String role) {
        String hashed = PasswordUtil.hashPassword(password);
        User user = new User(email, hashed, fullName, role != null ? role : "student");
        userRepository.save(user);
        
        User saved = userRepository.findByEmail(email);
        String token = tokenProvider.generateToken(saved.getEmail());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", Map.of("email", saved.getEmail(), "fullName", saved.getFullName()));
        return result;
    }

    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String token = tokenProvider.generateToken(user.getEmail());
        return Map.of("token", token, "user", Map.of("email", user.getEmail()));
    }

    public Map<String, Object> requestPasswordReset(String email) {
        String resetToken = UUID.randomUUID().toString().substring(0, 8);
        resetTokens.put(resetToken, email);
        emailService.sendResetCode(email, resetToken);
        return Map.of("message", "Reset code sent");
    }

    // signature: 2 strings
    public Map<String, Object> resetPassword(String token, String newPassword) {
        String email = resetTokens.get(token);
        if (email == null) throw new IllegalArgumentException("Invalid token");
        
        User user = userRepository.findByEmail(email);
        userRepository.updatePassword(user.getId(), PasswordUtil.hashPassword(newPassword));
        resetTokens.remove(token);
        return Map.of("message", "Success");
    }
}