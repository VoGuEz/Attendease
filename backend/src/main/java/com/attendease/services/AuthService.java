package com.attendease.services;

import com.attendease.models.User;
import com.attendease.repositories.UserRepository;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.PasswordUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AuthService {

    private final UserRepository userRepository = new UserRepository();

    public Map<String, Object> register(String email, String password, String fullName, String role) throws Exception {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required");
        if (!role.equals("student") && !role.equals("lecturer")) throw new IllegalArgumentException("Role must be student or lecturer");

        Optional<User> existing = userRepository.findByEmail(email.trim().toLowerCase());
        if (existing.isPresent()) throw new IllegalArgumentException("Email already registered");

        String hash = PasswordUtil.hashPassword(password);
        User user = new User(email.trim().toLowerCase(), hash, fullName.trim(), role);
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
}
