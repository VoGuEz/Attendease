package com.attendease.controller;

import com.attendease.entity.Attendance;
import com.attendease.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://127.0.0.1:5500"})
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/join")
    public ResponseEntity<Attendance> joinSession(@RequestBody Map<String, Object> request) {
        try {
            String sessionCode = (String) request.get("sessionCode");
            String fullname = (String) request.get("fullname");
            String indexNumber = (String) request.get("indexNumber");
            int level = ((Number) request.get("level")).intValue();
            Double latitude = ((Number) request.get("latitude")).doubleValue();
            Double longitude = ((Number) request.get("longitude")).doubleValue();

            Attendance attendance = attendanceService.joinSession(sessionCode, fullname, indexNumber, level, latitude, longitude);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Attendance>> getUserAttendance(Authentication authentication) {
        String userId = authentication.getName();
        List<Attendance> attendance = attendanceService.getUserAttendance(userId);
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/session/{sessionId}/csv")
    public ResponseEntity<byte[]> exportAttendanceCSV(@PathVariable String sessionId) {
        String csv = attendanceService.exportAttendanceCSV(sessionId);
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=\"attendance-" + sessionId + ".csv\"")
            .body(csvBytes);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<Attendance>> getAttendanceBySession(@PathVariable String sessionId) {
        List<Attendance> attendance = attendanceService.getAttendanceBySession(sessionId);
        return ResponseEntity.ok(attendance);
    }
}