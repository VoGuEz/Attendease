package com.attendease.handlers;

import com.attendease.models.Course;
import com.attendease.services.CourseService;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class CourseHandler {

    private final CourseService courseService = new CourseService();

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

            // POST /api/courses/{id}/enroll
            if (method.equals("POST") && path.matches("/api/courses/\\d+/enroll")) {
                int courseId = extractId(path, "/api/courses/", "/enroll");
                if (!"student".equals(role)) {
                    ResponseUtil.sendError(exchange, 403, "Only students can enroll");
                    return;
                }
                courseService.enrollStudent(userId, courseId);
                ResponseUtil.sendResponse(exchange, 200, Map.of("message", "Enrolled successfully"));
                return;
            }

            // GET /api/courses/{id}/sessions - handled by SessionHandler, but path could land here
            if (method.equals("GET") && path.matches("/api/courses/\\d+/sessions")) {
                int courseId = extractId(path, "/api/courses/", "/sessions");
                new SessionHandler().handleGetByCourse(exchange, courseId);
                return;
            }

            // GET /api/courses/{id}
            if (method.equals("GET") && path.matches("/api/courses/\\d+")) {
                int courseId = extractId(path, "/api/courses/", null);
                Course course = courseService.getCourseById(courseId);
                int enrolled = courseService.getEnrolledCount(courseId);
                JsonObject obj = ResponseUtil.getGson().toJsonTree(course).getAsJsonObject();
                obj.addProperty("enrolledCount", enrolled);
                ResponseUtil.sendResponse(exchange, 200, obj);
                return;
            }

            // GET /api/courses
            if (method.equals("GET") && path.equals("/api/courses")) {
                if ("lecturer".equals(role)) {
                    ResponseUtil.sendResponse(exchange, 200, courseService.getCoursesForLecturer(userId));
                } else {
                    ResponseUtil.sendResponse(exchange, 200, courseService.getCoursesForStudent(userId));
                }
                return;
            }

            // POST /api/courses
            if (method.equals("POST") && path.equals("/api/courses")) {
                if (!"lecturer".equals(role)) {
                    ResponseUtil.sendError(exchange, 403, "Only lecturers can create courses");
                    return;
                }
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String name = getStr(json, "courseName");
                String code = getStr(json, "courseCode");
                String description = getStr(json, "description");
                Course created = courseService.createCourse(userId, name, code, description);
                ResponseUtil.sendResponse(exchange, 201, created);
                return;
            }

            ResponseUtil.sendError(exchange, 404, "Not found");
        } catch (IllegalArgumentException e) {
            ResponseUtil.sendError(exchange, 400, e.getMessage());
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
