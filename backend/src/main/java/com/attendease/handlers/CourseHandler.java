package com.attendease.handlers;

import com.attendease.models.Course;
import com.attendease.services.CourseService;
import com.attendease.services.SessionService;
import com.attendease.utils.JwtUtil;
import com.attendease.utils.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class CourseHandler {
    private final CourseService courseService;
    private final SessionService sessionService;

    public CourseHandler(CourseService courseService, SessionService sessionService) {
        this.courseService = courseService;
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

            if (method.equals("POST") && path.matches("/api/courses/\\d+/enroll")) {
                int courseId = extractId(path, "/api/courses/", "/enroll");
                if (!"student".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only students can enroll"); return; }
                courseService.enrollStudent(userId, courseId);
                ResponseUtil.sendResponse(exchange, 200, Map.of("message", "Enrolled successfully"));
                return;
            }

            if (method.equals("GET") && path.matches("/api/courses/\\d+/sessions")) {
                int courseId = extractId(path, "/api/courses/", "/sessions");
                ResponseUtil.sendResponse(exchange, 200, sessionService.getSessionsForCourse(courseId));
                return;
            }

            if (method.equals("GET") && path.matches("/api/courses/\\d+")) {
                int courseId = extractId(path, "/api/courses/", null);
                Course course = courseService.getCourseById(courseId);
                int enrolled = courseService.getEnrolledCount(courseId);
                JsonObject obj = ResponseUtil.getGson().toJsonTree(course).getAsJsonObject();
                obj.addProperty("enrolledCount", enrolled);
                ResponseUtil.sendResponse(exchange, 200, obj);
                return;
            }

            if (method.equals("GET") && path.equals("/api/courses")) {
                if ("lecturer".equals(role)) {
                    ResponseUtil.sendResponse(exchange, 200, courseService.getCoursesForLecturer(userId));
                } else {
                    ResponseUtil.sendResponse(exchange, 200, courseService.getCoursesForStudent(userId));
                }
                return;
            }

            if (method.equals("POST") && path.equals("/api/courses")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only lecturers can create courses"); return; }
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                Course created = courseService.createCourse(userId, getStr(json, "courseName"), getStr(json, "courseCode"), getStr(json, "description"));
                ResponseUtil.sendResponse(exchange, 201, created);
                return;
            }

            if (method.equals("PUT") && path.matches("/api/courses/\\d+")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only lecturers can edit courses"); return; }
                int courseId = extractId(path, "/api/courses/", null);
                String body = ResponseUtil.readBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                Course updated = courseService.updateCourse(courseId, userId, getStr(json, "courseName"), getStr(json, "courseCode"), getStr(json, "description"));
                ResponseUtil.sendResponse(exchange, 200, updated);
                return;
            }

            if (method.equals("DELETE") && path.matches("/api/courses/\\d+")) {
                if (!"lecturer".equals(role)) { ResponseUtil.sendError(exchange, 403, "Only lecturers can delete courses"); return; }
                int courseId = extractId(path, "/api/courses/", null);
                courseService.deleteCourse(courseId, userId);
                ResponseUtil.sendResponse(exchange, 200, Map.of("message", "Course deleted successfully"));
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