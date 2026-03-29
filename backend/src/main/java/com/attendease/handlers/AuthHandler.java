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

    private final AuthService authService;
    private final UserRepository userRepository = new UserRepository();

    public AuthHandler(AuthService authService) {
        this.authService = authService;
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
            if (fullName == null || fullName.isBlank()) {
                ResponseUtil.sendError(exchange, 400, "Full name is required");
                return;
            }

            boolean updated = userRepository.updateFullName(userId, fullName.trim());
            if (!updated) {
                ResponseUtil.sendError(exchange, 404, "User not found");
                return;
            }

            String role  = JwtUtil.getRole(token);
            String email = JwtUtil.validateToken(token).get("email", String.class);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", userId);
            userInfo.put("email", email);
            userInfo.put("fullName", fullName.trim());
            userInfo.put("role", role);
            ResponseUtil.sendResponse(exchange, 200, userInfo);
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
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Failed to process reset request: " + e.getMessage());
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
            Map<String, Object> result = authService.resetPassword(token, newPassword);
            ResponseUtil.sendResponse(exchange, 200, result);
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    private String getStringOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
}