package com.attendease.services;

import com.attendease.models.Session;
import com.attendease.repositories.SessionRepository;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;

public class SessionService {

    private final SessionRepository sessionRepository = new SessionRepository();

    public Session createSession(int courseId, String date, String startTime, String endTime) throws SQLException {
        if (date == null || date.isBlank()) throw new IllegalArgumentException("Session date is required");
        if (startTime == null || startTime.isBlank()) throw new IllegalArgumentException("Start time is required");
        if (endTime == null || endTime.isBlank()) throw new IllegalArgumentException("End time is required");

        Session session = new Session(
                courseId,
                Date.valueOf(date),
                Time.valueOf(startTime.length() == 5 ? startTime + ":00" : startTime),
                Time.valueOf(endTime.length() == 5 ? endTime + ":00" : endTime),
                "upcoming"
        );
        return sessionRepository.save(session);
    }

    public List<Session> getSessionsForCourse(int courseId) throws SQLException {
        return sessionRepository.findByCourseId(courseId);
    }

    public void updateStatus(int sessionId, String status) throws SQLException {
        if (!List.of("upcoming", "active", "completed").contains(status)) {
            throw new IllegalArgumentException("Invalid status. Must be upcoming, active, or completed");
        }
        sessionRepository.updateStatus(sessionId, status);
    }

    public List<Session> getActiveSessions() throws SQLException {
        return sessionRepository.findActiveSessions();
    }

    public List<Session> getActiveSessionsForStudent(int studentId) throws SQLException {
        return sessionRepository.findActiveSessionsForStudent(studentId);
    }

    public Session getSessionById(int id) throws SQLException {
        return sessionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    public Session updateSession(int sessionId, String date, String startTime, String endTime) throws SQLException {
        sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (date == null || date.isBlank()) throw new IllegalArgumentException("Session date is required");
        if (startTime == null || startTime.isBlank()) throw new IllegalArgumentException("Start time is required");
        if (endTime == null || endTime.isBlank()) throw new IllegalArgumentException("End time is required");
        sessionRepository.update(sessionId,
                Date.valueOf(date),
                Time.valueOf(startTime.length() == 5 ? startTime + ":00" : startTime),
                Time.valueOf(endTime.length() == 5 ? endTime + ":00" : endTime));
        return sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    public void deleteSession(int sessionId) throws SQLException {
        sessionRepository.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        sessionRepository.delete(sessionId);
    }
}
