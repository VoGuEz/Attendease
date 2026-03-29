package com.attendease.handlers;

import com.attendease.models.Attendance;
import com.attendease.services.AttendanceService;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class AttendanceHandler {
    private final AttendanceService attendanceService;

    public AttendanceHandler(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        String token = JwtUtil.extractTokenFromHeader(exchange.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { ResponseUtil.sendError(exchange, 401, "Unauthorized"); return; }

        try {
            int userId = JwtUtil.getUserId(token);
            String role = JwtUtil.getRole(token);

            // POST /api/attendance/join — student joins with code
            if (method.equals("POST") && path.equals("/api/attendance/join")) {
                if (!"student".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only students can join sessions"); return; }

                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                if (!json.has("sessionId") || !json.has("code")) {
                    ResponseUtil.sendError(exchange, 400, "sessionId and code are required"); return;
                }

                int sessionId = json.get("sessionId").getAsInt();
                String code = getStr(json, "code");
                String fullName = getStr(json, "fullName");
                String indexNumber = getStr(json, "indexNumber");
                String level = getStr(json, "level");
                Double latitude = json.has("latitude") && !json.get("latitude").isJsonNull() ? json.get("latitude").getAsDouble() : null;
                Double longitude = json.has("longitude") && !json.get("longitude").isJsonNull() ? json.get("longitude").getAsDouble() : null;

                // Validate code before attempting to join
                if (!attendanceService.validateSessionCode(sessionId, code)) {
                    ResponseUtil.sendError(exchange, 403, "Invalid or expired session code"); return;
                }

                Map<String, Object> result = attendanceService.joinSession(
                        userId, sessionId, fullName, indexNumber, level, latitude, longitude);
                ResponseUtil.sendResponse(exchange, 200, result);
                return;
            }

            // GET /api/attendance/stats — student's own stats
            if (method.equals("GET") && path.equals("/api/attendance/stats")) {
                ResponseUtil.sendResponse(exchange, 200, attendanceService.getStudentStats(userId));
                return;
            }

            // GET /api/attendance/session/{id}/validate?code=xxxxxx
            if (method.equals("GET") && path.startsWith("/api/attendance/session/") && path.endsWith("/validate")) {
                if (!"student".equals(role)) { ResponseUtil.sendError(exchange, 403, "Forbidden"); return; }

                String[] parts = path.split("/");
                int sessionId = Integer.parseInt(parts[4]);

                String code = null;
                String query = exchange.getRequestURI().getQuery();
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            code = URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }

                if (attendanceService.validateSessionCode(sessionId, code)) {
                    ResponseUtil.sendResponse(exchange, 200, Map.of("valid", true));
                } else {
                    ResponseUtil.sendError(exchange, 403, "Invalid or expired session code");
                }
                return;
            }

            // GET /api/attendance/session/{id} — lecturer views attendance list
            if (method.equals("GET") && path.startsWith("/api/attendance/session/")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Forbidden"); return; }
                int sessionId = Integer.parseInt(path.substring("/api/attendance/session/".length()));
                List<Attendance> list = attendanceService.getAttendanceForSession(sessionId, userId);
                ResponseUtil.sendResponse(exchange, 200, list);
                return;
            }

            ResponseUtil.sendError(exchange, 404, "Not found");
        } catch (SecurityException e) {
            ResponseUtil.sendError(exchange, 403, e.getMessage());
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private String getStr(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) return json.get(key).getAsString();
        return null;
    }
}