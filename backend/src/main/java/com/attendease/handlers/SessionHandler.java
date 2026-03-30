package com.attendease.handlers;

import com.attendease.models.Session;
import com.attendease.services.SessionService;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SessionHandler {
    private final SessionService sessionService;

    public SessionHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        String token = JwtUtil.extractTokenFromHeader(exchange.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { ResponseUtil.sendError(exchange, 401, "Unauthorized"); return; }

        try {
            int userId = JwtUtil.getUserId(token);
            String role = JwtUtil.getRole(token);

            // GET /api/sessions/{id}/code — lecturer fetches their own session code
            if (method.equals("GET") && path.matches("/api/sessions/\\d+/code")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Forbidden"); return; }
                int sessionId = extractId(path, "/api/sessions/", "/code");
                String code = sessionService.getSessionCodeForLecturer(sessionId, userId);
                if (code == null) {
                    ResponseUtil.sendError(exchange, 403, "Access denied or session has no code yet");
                    return;
                }
                ResponseUtil.sendResponse(exchange, 200, Map.of("code", code));
                return;
            }

            // PUT /api/sessions/{id}/status — update session status
            if (method.equals("PUT") && path.matches("/api/sessions/\\d+/status")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Forbidden"); return; }
                int sessionId = extractId(path, "/api/sessions/", "/status");
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String status = json.has("status") ? json.get("status").getAsString() : null;
                sessionService.updateStatus(sessionId, status);
                ResponseUtil.sendResponse(exchange, 200, Map.of("message", "Status updated"));
                return;
            }

            // GET /api/sessions/active
            if (method.equals("GET") && path.equals("/api/sessions/active")) {
                List<Session> sessions = "student".equals(role)
                    ? sessionService.getActiveSessionsForStudent(userId)
                    : sessionService.getActiveSessions();
                ResponseUtil.sendResponse(exchange, 200, sessions);
                return;
            }

            // POST /api/sessions — create session
            if (method.equals("POST") && path.equals("/api/sessions")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only lecturers can create sessions"); return; }
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                int courseId = json.get("courseId").getAsInt();
                Session created = sessionService.createSession(courseId, getStr(json, "sessionDate"), getStr(json, "startTime"), getStr(json, "endTime"));
                ResponseUtil.sendResponse(exchange, 201, created);
                return;
            }

            // PUT /api/sessions/{id} — edit session details
            if (method.equals("PUT") && path.matches("/api/sessions/\\d+") && !path.contains("/status") && !path.contains("/code")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only lecturers can edit sessions"); return; }
                int sessionId = Integer.parseInt(path.substring("/api/sessions/".length()));
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                Session updated = sessionService.updateSession(sessionId, getStr(json, "sessionDate"), getStr(json, "startTime"), getStr(json, "endTime"));
                ResponseUtil.sendResponse(exchange, 200, updated);
                return;
            }

            // DELETE /api/sessions/{id}
            if (method.equals("DELETE") && path.matches("/api/sessions/\\d+")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only lecturers can delete sessions"); return; }
                int sessionId = Integer.parseInt(path.substring("/api/sessions/".length()));
                sessionService.deleteSession(sessionId);
                ResponseUtil.sendResponse(exchange, 200, Map.of("message", "Session deleted successfully"));
                return;
            }

            ResponseUtil.sendError(exchange, 404, "Not found");
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    public void handleGetByCourse(HttpExchange exchange, int courseId) throws IOException {
        try {
            ResponseUtil.sendResponse(exchange, 200, sessionService.getSessionsForCourse(courseId));
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private int extractId(String path, String prefix, String suffix) {
        String part = path.substring(prefix.length());
        if (suffix != null) part = part.substring(0, part.indexOf(suffix));
        return Integer.parseInt(part);
    }

    private String getStr(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) return json.get(key).getAsString();
        return null;
    }
}