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

}
