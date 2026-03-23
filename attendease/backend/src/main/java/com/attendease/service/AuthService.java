package com.attendease.service;

import com.attendease.entity.User;
import com.attendease.repository.UserRepository;
import com.attendease.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public void signup(String fullname, String email, String password, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setFullname(fullname);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.valueOf(role));

        userRepository.save(user);
    }

    public Map<String, Object> login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("fullname", user.getFullname());
        response.put("role", user.getRole().toString());

        return response;
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }
}