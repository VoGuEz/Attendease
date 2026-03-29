package com.attendease.handlers;

import com.attendease.models.User;
import com.attendease.repositories.UserRepository;
import com.attendease.services.AuthService;
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

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthHandler(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    public void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String body = ResponseUtil.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String email    = getStringOrNull(json, "email");
            String password = getStringOrNull(json, "password");
            String fullName = getStringOrNull(json, "fullName");
            String role     = getStringOrNull(json, "role");

            // Correctly calling the 4-parameter register method
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
            String email    = getStringOrNull(json, "email");
            String password = getStringOrNull(json, "password");

            Map<String, Object> result = authService.login(email, password);
            ResponseUtil.sendResponse(exchange, 200, result);
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    public void handleUpdateName(HttpExchange exchange) throws IOException {
        if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        // Since we are refactoring, for now, we will handle basic updates. 
        // Real token validation should happen in a Middleware/Filter later.
        try {
            String body = ResponseUtil.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            
            // For now, we expect ID and Name in the body for the update
            int userId = json.get("userId").getAsInt(); 
            String fullName = getStringOrNull(json, "fullName");

            if (fullName == null || fullName.isBlank()) {
                ResponseUtil.sendError(exchange, 400, "Full name is required");
                return;
            }

            boolean updated = userRepository.updateFullName(userId, fullName.trim());
            if (!updated) {
                ResponseUtil.sendError(exchange, 404, "User not found");
                return;
            }

            ResponseUtil.sendResponse(exchange, 200, Map.of("message", "Name updated successfully"));
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Error updating name: " + e.getMessage());
        }
    }

    public void handleGetStudents(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
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
            ResponseUtil.sendError(exchange, 500, "Error fetching students");
        }
    }

    public void handleRequestReset(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String body = ResponseUtil.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String email = getStringOrNull(json, "email");
            
            Map<String, Object> result = authService.requestPasswordReset(email);
            ResponseUtil.sendResponse(exchange, 200, result);
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Reset request failed");
        }
    }

    public void handleResetPassword(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String body = ResponseUtil.readBody(exchange);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String token       = getStringOrNull(json, "token");
            String newPassword = getStringOrNull(json, "newPassword");

            // Correctly calling the 2-parameter resetPassword method
            Map<String, Object> result = authService.resetPassword(token, newPassword);
            ResponseUtil.sendResponse(exchange, 200, result);
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Password reset failed");
        }
    }

    private String getStringOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
}