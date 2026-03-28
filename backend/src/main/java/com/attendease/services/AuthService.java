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

    public User register(String name, String email, String password, String role) {
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already registered");
        }
        String hashed = passwordUtil.hashPassword(password);
        User user = new User(UUID.randomUUID().toString(), name, email, hashed, role);
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
}
            if (optUser.isPresent()) {
                User user = optUser.get();
                user.setEmailVerified(true);
                userRepository.setEmailVerified(normalizedEmail, true);
                verificationCodes.remove(normalizedEmail);
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> login(String email, String password) throws Exception {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        Optional<User> optUser = userRepository.findByEmail(email.trim().toLowerCase());
        if (optUser.isEmpty()) throw new IllegalArgumentException("Invalid email or password");

        User user = optUser.get();
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Please verify your email before signing in.");
        }
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
