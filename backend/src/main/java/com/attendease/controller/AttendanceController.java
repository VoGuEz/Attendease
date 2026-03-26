package com.attendease.controller;

import com.attendease.dto.AttendanceDTO;
import com.attendease.dto.StudentStatsDTO;
import com.attendease.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/join")
    public ResponseEntity<AttendanceDTO> joinSession(
        @RequestBody Map<String, Object> body,
        Authentication auth
    ) {
        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        Double lat = body.get("latitude")  != null ? Double.valueOf(body.get("latitude").toString())  : null;
        Double lng = body.get("longitude") != null ? Double.valueOf(body.get("longitude").toString()) : null;
        return ResponseEntity.ok(attendanceService.joinSession(sessionId, auth.getName(), lat, lng));
    }

    @PostMapping("/leave")
    public ResponseEntity<AttendanceDTO> leaveSession(
        @RequestBody Map<String, Object> body,
        Authentication auth
    ) {
        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        return ResponseEntity.ok(attendanceService.leaveSession(sessionId, auth.getName()));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AttendanceDTO>> getSessionAttendance(@PathVariable Long sessionId) {
        return ResponseEntity.ok(attendanceService.getAttendanceBySession(sessionId));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<AttendanceDTO>> getStudentAttendance(@PathVariable Long studentId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByStudent(studentId));
    }

    @GetMapping("/statistics/{studentId}")
    public ResponseEntity<StudentStatsDTO> getStudentStats(@PathVariable Long studentId) {
        return ResponseEntity.ok(attendanceService.getStudentStats(studentId));
    }

    @GetMapping("/progress/{studentId}")
    public ResponseEntity<StudentStatsDTO> getStudentProgress(@PathVariable Long studentId) {
        return ResponseEntity.ok(attendanceService.getStudentStats(studentId));
    }
}
