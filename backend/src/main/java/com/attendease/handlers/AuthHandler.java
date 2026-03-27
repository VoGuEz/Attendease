package com.attendease.handlers;

import com.attendease.models.User;
import com.attendease.repositories.UserRepository;
import com.attendease.services.AuthService;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthHandler {

    private final AuthService authService = new AuthService();
    private final UserRepository userRepository = new UserRepository();

    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String body = ResponseUtil.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String email = getStringOrNull(json, "email");
            String password = getStringOrNull(json, "password");
            String fullName = getStringOrNull(json, "fullName");
            String role = getStringOrNull(json, "role");

            Map<String, Object> result = authService.register(email, password, fullName, role);
            ResponseUtil.sendResponse(exchange, 201, result);
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String body = ResponseUtil.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String email = getStringOrNull(json, "email");
            String password = getStringOrNull(json, "password");

            Map<String, Object> result = authService.login(email, password);
            ResponseUtil.sendResponse(exchange, 200, result);
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    public void handleGetStudents(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String token = JwtUtil.extractTokenFromHeader(authHeader);
            if (token == null) {
                ResponseUtil.sendError(exchange, 401, "Unauthorized");
                return;
            }
            String role = JwtUtil.getRole(token);
            if (!"lecturer".equals(role)) {
                ResponseUtil.sendError(exchange, 403, "Forbidden");
                return;
            }

            List<User> students = userRepository.findAllStudents();
            List<Map<String, Object>> result = students.stream().map(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("email", u.getEmail());
                m.put("fullName", u.getFullName());
                return m;
            }).collect(Collectors.toList());

            ResponseUtil.sendResponse(exchange, 200, result);
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    public void handleUpdateProfile(HttpExchange exchange) throws IOException {
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String token = JwtUtil.extractTokenFromHeader(authHeader);
            if (token == null) {
                ResponseUtil.sendError(exchange, 401, "Unauthorized");
                return;
            }

            int userId = JwtUtil.getUserId(token);

            String body = ResponseUtil.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String fullName = getStringOrNull(json, "fullName");
            String email = getStringOrNull(json, "email");

            if ((fullName == null || fullName.isBlank()) && (email == null || email.isBlank())) {
                ResponseUtil.sendError(exchange, 400, "At least one field is required");
                return;
            }

            java.util.Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                ResponseUtil.sendError(exchange, 404, "User not found");
                return;
            }

            User user = userOpt.get();
            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName.trim());
            }
            if (email != null && !email.isBlank()) {
                String newEmail = email.trim().toLowerCase();
                java.util.Optional<User> existing = userRepository.findByEmail(newEmail);
                if (existing.isPresent() && existing.get().getId() != userId) {
                    ResponseUtil.sendError(exchange, 400, "Email already in use");
                    return;
                }
                user.setEmail(newEmail);
            }

            userRepository.update(user);

            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("email", user.getEmail());
            result.put("fullName", user.getFullName());
            result.put("role", user.getRole());
            ResponseUtil.sendResponse(exchange, 200, result);
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 401, "Invalid or expired token");
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private String getStringOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
}
