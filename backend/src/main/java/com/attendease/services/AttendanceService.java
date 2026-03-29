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
    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private static final int LATE_THRESHOLD_MINUTES = 15;

    public AttendanceService(AttendanceRepository attendanceRepository, SessionRepository sessionRepository) {
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
    }

    public boolean validateSessionCode(int sessionId, String code) throws SQLException {
        if (code == null || code.isBlank()) return false;
        return sessionRepository.validateCode(sessionId, code);
    }

    public Map<String, Object> joinSession(int studentId, int sessionId, String fullName,
            String indexNumber, String level, Double latitude, Double longitude) throws SQLException {
        if (attendanceRepository.existsBySessionAndStudent(sessionId, studentId))
            throw new IllegalArgumentException("You have already joined this session");
        if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("Full name is required");
        if (indexNumber == null || indexNumber.isBlank()) throw new IllegalArgumentException("Index number is required");
        if (level == null || level.isBlank()) throw new IllegalArgumentException("Level is required");
        if (latitude == null || longitude == null) throw new IllegalArgumentException("Location access is required");
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Invalid GPS coordinates");

        String status = "present";
        try {
            Session session = sessionRepository.findById(sessionId).orElse(null);
            if (session != null && session.getStartTime() != null) {
                if (LocalTime.now().isAfter(session.getStartTime().toLocalTime().plusMinutes(LATE_THRESHOLD_MINUTES)))
                    status = "late";
            }
        } catch (Exception ignored) {}

        Attendance attendance = new Attendance(sessionId, studentId, status);
        attendance.setSubmittedFullName(fullName.trim());
        attendance.setSubmittedIndexNumber(indexNumber.trim());
        attendance.setSubmittedLevel(level.trim());
        attendance.setLatitude(latitude);
        attendance.setLongitude(longitude);
        attendanceRepository.save(attendance);

        Map<String, Object> response = new HashMap<>();
        response.put("attendance", attendance);
        response.put("stats", attendanceRepository.getStudentStats(studentId));
        return response;
    }

    public List<Attendance> getAttendanceForSession(int sessionId, int lecturerId) throws SQLException {
        if (!sessionRepository.isOwnedByLecturer(sessionId, lecturerId)) {
            throw new SecurityException("Access denied: this session does not belong to you");
        }
        return attendanceRepository.findBySessionId(sessionId);
    }

    public Map<String, Object> getStudentStats(int studentId) throws SQLException {
        return attendanceRepository.getStudentStats(studentId);
    }
}