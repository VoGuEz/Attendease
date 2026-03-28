package com.attendease.services;

import com.attendease.models.User;
import com.attendease.repositories.UserRepository;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.PasswordUtil;

import java.util.UUID;

public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
    }

    public User register(String email, String password, String fullName, String role) {
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already registered");
        }
        String hashed = passwordUtil.hashPassword(password);
        User user = new User(email, hashed, fullName, role);
        userRepository.save(user);
        return user;
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null || !passwordUtil.verifyPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return user;
    }

    public String generateToken(User user) {
        return jwtUtil.generateToken(user);
    }

        User user = optUser.get();
        String token = UUID.randomUUID().toString();
        // Token valid for 15 minutes
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 15 * 60 * 1000);
        userRepository.saveResetToken(user.getId(), token, expiresAt);

        // Send the reset code via email
        emailService.sendResetCode(normalizedEmail, token, user.getFullName());

        return response;
    }

    public Map<String, Object> resetPassword(String token, String newPassword) throws Exception {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Reset token is required");
        if (newPassword == null || newPassword.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");

        Optional<int[]> result = userRepository.findValidResetToken(token);
        if (result.isEmpty()) throw new IllegalArgumentException("Invalid or expired reset token");

        int userId = result.get()[0];
        int tokenId = result.get()[1];

        String hash = PasswordUtil.hashPassword(newPassword);
        userRepository.updatePassword(userId, hash);
        userRepository.markResetTokenUsed(tokenId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset successfully. You can now log in with your new password.");
        return response;
    }
}
