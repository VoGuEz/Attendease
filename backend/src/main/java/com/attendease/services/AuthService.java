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

    public Map<String, Object> register(String email, String password, String fullName, String role) {
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hashed = PasswordUtil.hashPassword(password);
        User user = new User(email, hashed, fullName, role != null ? role : "student");
        userRepository.save(user);
        
        User saved = userRepository.findByEmail(email);
        // PASSING ID AND ROLE TO TOKEN
        String token = tokenProvider.generateToken(saved.getId(), saved.getEmail(), saved.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userToMap(saved));
        return result;
    }

    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // PASSING ID AND ROLE TO TOKEN
        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        
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
            emailService.sendResetCode(email, resetToken);
        }
        return Map.of("message", "If that email exists, a reset code has been sent.");
    }

    public Map<String, Object> resetPassword(String token, String newPassword) {
        String email = resetTokens.get(token);
        if (email == null) throw new IllegalArgumentException("Invalid or expired token");
        
        User user = userRepository.findByEmail(email);
        userRepository.updatePassword(user.getId(), PasswordUtil.hashPassword(newPassword));
        resetTokens.remove(token);
        return Map.of("message", "Password reset successfully");
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("fullName", user.getFullName());
        m.put("role", user.getRole());
        return m;
    }
}