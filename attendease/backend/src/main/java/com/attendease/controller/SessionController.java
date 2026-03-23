package com.attendease.controller;

import com.attendease.entity.Session;
import com.attendease.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sessions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://127.0.0.1:5500"})
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @PostMapping
    public ResponseEntity<Session> createSession(@RequestBody Map<String, Object> request, Authentication authentication) {
        String lecturerId = authentication.getName();
        String title = (String) request.get("title");
        int durationMinutes = ((Number) request.get("durationMinutes")).intValue();

        Session session = sessionService.createSession(title, durationMinutes, lecturerId);
        return ResponseEntity.ok(session);
    }

    @GetMapping
    public ResponseEntity<List<Session>> getUserSessions(Authentication authentication) {
        String lecturerId = authentication.getName();
        List<Session> sessions = sessionService.getSessionsByLecturer(lecturerId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Session> getSessionByCode(@PathVariable String code) {
        Session session = sessionService.getSessionByCode(code);
        return ResponseEntity.ok(session);
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<String> endSession(@PathVariable String id) {
        sessionService.endSession(id);
        return ResponseEntity.ok("Session ended");
    }
}