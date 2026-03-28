package com.attendease.services;

import com.attendease.models.Attendance;
import com.attendease.models.Session;
import com.attendease.repositories.AttendanceRepository;
import com.attendease.repositories.SessionRepository;

import java.sql.SQLException;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceService {

    private final AttendanceRepository attendanceRepository = new AttendanceRepository();
    private final SessionRepository sessionRepository = new SessionRepository();

    private static final int LATE_THRESHOLD_MINUTES = 15;

    public Map<String, Object> joinSession(
            int studentId,
            int sessionId,
            String fullName,
            String indexNumber,
            String level,
            Double latitude,
            Double longitude
    ) throws SQLException {
        if (attendanceRepository.existsBySessionAndStudent(sessionId, studentId)) {
            throw new IllegalArgumentException("You have already joined this session");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required before joining a session");
        }
        if (indexNumber == null || indexNumber.isBlank()) {
            throw new IllegalArgumentException("Index number is required before joining a session");
        }
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("Level is required before joining a session");
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Location access is required before joining a session");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid GPS coordinates supplied");
        }

        // Determine if student is late based on session start time
        String status = "present";
        try {
            Session session = sessionRepository.findById(sessionId).orElse(null);
            if (session != null && session.getStartTime() != null) {
                LocalTime sessionStart = session.getStartTime().toLocalTime();
                LocalTime now = LocalTime.now();
                if (now.isAfter(sessionStart.plusMinutes(LATE_THRESHOLD_MINUTES))) {
                    status = "late";
                }
            }
        } catch (Exception e) {
            // If we can't determine lateness, default to present
        }

        Attendance attendance = new Attendance(sessionId, studentId, status);
        attendance.setSubmittedFullName(fullName.trim());
        attendance.setSubmittedIndexNumber(indexNumber.trim());
        attendance.setSubmittedLevel(level.trim());
        attendance.setLatitude(latitude);
        attendance.setLongitude(longitude);
        attendanceRepository.save(attendance);

        Map<String, Object> stats = attendanceRepository.getStudentStats(studentId);

        Map<String, Object> response = new HashMap<>();
        response.put("attendance", attendance);
        response.put("stats", stats);
        return response;
    }

    public List<Attendance> getAttendanceForSession(int sessionId) throws SQLException {
        return attendanceRepository.findBySessionId(sessionId);
    }

    public Map<String, Object> getStudentStats(int studentId) throws SQLException {
        return attendanceRepository.getStudentStats(studentId);
    }
}
