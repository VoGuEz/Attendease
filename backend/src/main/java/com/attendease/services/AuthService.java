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

    // THIS IS THE CRITICAL FIX: The parameters must match Main.java exactly
    public AuthService(UserRepository userRepository, EmailService emailService, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.tokenProvider = tokenProvider;
    }

    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Use the new tokenProvider instead of the old JwtUtil
        String token = tokenProvider.generateToken(user.getEmail());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userToMap(user));
        return result;
    }

    public Map<String, Object> requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            String resetToken = UUID.randomUUID().toString().substring(0, 8);
            resetTokens.put(resetToken, email);
            
            // This now calls your real EmailService
            emailService.sendResetCode(email, resetToken);
        }
        return Map.of("message", "If that email exists, a reset code has been sent.");
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("fullName", user.getFullName());
        m.put("role", user.getRole());
        return m;
    }
    
    // Include your resetPassword and other methods here...
}