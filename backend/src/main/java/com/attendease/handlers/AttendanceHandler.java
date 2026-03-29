package com.attendease.handlers;

import com.attendease.models.Attendance;
import com.attendease.services.AttendanceService;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
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

            if (method.equals("POST") && path.equals("/api/attendance/join")) {
                if (!"student".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only students can join sessions"); return; }
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                int sessionId = json.get("sessionId").getAsInt();
                String fullName = getStr(json, "fullName");
                String indexNumber = getStr(json, "indexNumber");
                String level = getStr(json, "level");
                Double latitude = json.has("latitude") && !json.get("latitude").isJsonNull() ? json.get("latitude").getAsDouble() : null;
                Double longitude = json.has("longitude") && !json.get("longitude").isJsonNull() ? json.get("longitude").getAsDouble() : null;
                Map<String, Object> result = attendanceService.joinSession(userId, sessionId, fullName, indexNumber, level, latitude, longitude);
                ResponseUtil.sendResponse(exchange, 200, result);
                return;
            }

            if (method.equals("GET") && path.equals("/api/attendance/stats")) {
                ResponseUtil.sendResponse(exchange, 200, attendanceService.getStudentStats(userId));
                return;
            }

            if (method.equals("GET") && path.matches("/api/attendance/session/\\d+")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Forbidden"); return; }
                int sessionId = Integer.parseInt(path.substring("/api/attendance/session/".length()));
                // userId here is the authenticated lecturer — ownership is verified inside the service
                List<Attendance> list = attendanceService.getAttendanceForSession(sessionId, userId);
                ResponseUtil.sendResponse(exchange, 200, list);
                return;
            }

            ResponseUtil.sendError(exchange, 404, "Not found");
        } catch (SecurityException e) {
            // Lecturer tried to access a session that belongs to another lecturer
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