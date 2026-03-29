package com.attendease.services;

import com.attendease.models.User;
import com.attendease.repositories.UserRepository;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.PasswordUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;

    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
    }

    public Map<String, Object> register(String email, String password, String fullName, String role) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required");
        if (role == null || role.isBlank()) role = "student";

        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hashed = passwordUtil.hashPassword(password);
        User user = new User(email, hashed, fullName, role);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userToMap(user));
        return result;
    }

    public Map<String, Object> login(String email, String password) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        User user = userRepository.findByEmail(email);
        if (user == null || !passwordUtil.verifyPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userToMap(user));
        return result;
    }

    public Map<String, Object> requestPasswordReset(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return Map.of("message", "If that email exists, a reset code has been sent.");
        }

        String resetToken = java.util.UUID.randomUUID().toString();
        resetTokens.put(resetToken, email);

        System.out.println("[AuthService] Password reset token for " + email + ": " + resetToken);

        return Map.of("message", "If that email exists, a reset code has been sent.");
    }

    public Map<String, Object> resetPassword(String resetToken, String newPassword) {
        if (resetToken == null || resetToken.isBlank()) throw new IllegalArgumentException("Reset token is required");
        if (newPassword == null || newPassword.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");

        String email = resetTokens.get(resetToken);
        if (email == null) throw new IllegalArgumentException("Invalid or expired reset token");

        User user = userRepository.findByEmail(email);
        if (user == null) throw new IllegalArgumentException("User not found");

        String hashed = passwordUtil.hashPassword(newPassword);
        userRepository.updatePassword(user.getId(), hashed);

        resetTokens.remove(resetToken);

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