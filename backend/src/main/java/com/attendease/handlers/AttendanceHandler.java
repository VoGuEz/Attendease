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

    private final AttendanceService attendanceService = new AttendanceService();

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        if (token == null) {
            ResponseUtil.sendError(exchange, 401, "Unauthorized");
            return;
        }

        try {
            int userId = JwtUtil.getUserId(token);
            String role = JwtUtil.getRole(token);

            // POST /api/attendance/join
            if (method.equals("POST") && path.equals("/api/attendance/join")) {
                if (!"student".equals(role)) {
                    ResponseUtil.sendError(exchange, 403, "Only students can join sessions");
                    return;
                }
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                int sessionId = json.get("sessionId").getAsInt();
                Map<String, Object> result = attendanceService.joinSession(userId, sessionId);
                ResponseUtil.sendResponse(exchange, 200, result);
                return;
            }

            // GET /api/attendance/stats
            if (method.equals("GET") && path.equals("/api/attendance/stats")) {
                Map<String, Object> stats = attendanceService.getStudentStats(userId);
                ResponseUtil.sendResponse(exchange, 200, stats);
                return;
            }

            // GET /api/attendance/session/{id}
            if (method.equals("GET") && path.matches("/api/attendance/session/\\d+")) {
                if (!"lecturer".equals(role)) {
                    ResponseUtil.sendError(exchange, 403, "Forbidden");
                    return;
                }
                int sessionId = Integer.parseInt(path.substring("/api/attendance/session/".length()));
                List<Attendance> list = attendanceService.getAttendanceForSession(sessionId);
                ResponseUtil.sendResponse(exchange, 200, list);
                return;
            }

            ResponseUtil.sendError(exchange, 404, "Not found");
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}
