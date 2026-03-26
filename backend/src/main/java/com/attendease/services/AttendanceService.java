package com.attendease.services;

import com.attendease.models.Attendance;
import com.attendease.repositories.AttendanceRepository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceService {

    private final AttendanceRepository attendanceRepository = new AttendanceRepository();

    public Map<String, Object> joinSession(int studentId, int sessionId) throws SQLException {
        if (attendanceRepository.existsBySessionAndStudent(sessionId, studentId)) {
            throw new IllegalArgumentException("You have already joined this session");
        }

        Attendance attendance = new Attendance(sessionId, studentId, "present");
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
