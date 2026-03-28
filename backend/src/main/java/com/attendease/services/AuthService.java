package com.attendease.services;

import com.attendease.models.User;
import com.attendease.repositories.UserRepository;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.PasswordUtil;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,63}$"
    );
        private static final Set<String> RESERVED_EMAIL_DOMAINS = Set.of(
            "example.com",
            "example.org",
            "example.net",
            "localhost",
            "localdomain"
        );
        private static final Set<String> RESERVED_EMAIL_SUFFIXES = Set.of(
            ".example",
            ".invalid",
            ".localhost",
            ".test"
        );

    private final UserRepository userRepository = new UserRepository();
    private final EmailService emailService = new EmailService();

    public Map<String, Object> register(String email, String password, String fullName, String role) throws Exception {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        String normalizedEmail = email.trim().toLowerCase();
        if (!isAllowedEmail(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "Enter a valid email address with a real public domain (for example @gmail.com, @yahoo.com, school.edu, or company.com)."
            );
        }
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required");
        if (!role.equals("student") && !role.equals("lecturer")) throw new IllegalArgumentException("Role must be student or lecturer");

        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) throw new IllegalArgumentException("Email already registered");

        String hash = PasswordUtil.hashPassword(password);
        User user = new User(normalizedEmail, hash, fullName.trim(), role);
        userRepository.save(user);

        String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return buildResponse(user, token);
    }

    public Map<String, Object> login(String email, String password) throws Exception {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        Optional<User> optUser = userRepository.findByEmail(email.trim().toLowerCase());
        if (optUser.isEmpty()) throw new IllegalArgumentException("Invalid email or password");

        User user = optUser.get();
        if (!PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return buildResponse(user, token);
    }

    private Map<String, Object> buildResponse(User user, String token) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("role", user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userInfo);
        return response;
    }

    private boolean isAllowedEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf('@') + 1);
        if ("gmail.com".equals(domain) || "yahoo.com".equals(domain)) {
            return true;
        }

        if (RESERVED_EMAIL_DOMAINS.contains(domain)) {
            return false;
        }

        for (String suffix : RESERVED_EMAIL_SUFFIXES) {
            if (domain.endsWith(suffix)) {
                return false;
            }
        }

        // Accept globally valid domain-style emails (e.g., school.edu, company.co.uk).
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".");
    }

    public Map<String, Object> requestPasswordReset(String email) throws Exception {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> optUser = userRepository.findByEmail(normalizedEmail);
        // Always return success to prevent email enumeration
        Map<String, Object> response = new HashMap<>();
        response.put("message", "If an account with that email exists, a reset code has been sent to your email.");

        if (optUser.isEmpty()) return response;

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
