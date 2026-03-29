package com.attendease.services;

import com.attendease.models.Session;
import com.attendease.repositories.SessionRepository;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;
import java.util.Random;

public class SessionService {
    private final SessionRepository sessionRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    public SessionService(SessionRepository sessionRepository, EmailService emailService) {
        this.sessionRepository = sessionRepository;
        this.emailService = emailService;
    }

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

    /**
     * Updates session status. When going active, generates a 6-digit code,
     * saves it, and emails it to all enrolled students.
     */
    public void updateStatus(int sessionId, String status) throws SQLException {
        if (!List.of("upcoming", "active", "completed").contains(status))
            throw new IllegalArgumentException("Invalid status");

        sessionRepository.updateStatus(sessionId, status);

        if ("active".equals(status)) {
            String code = String.format("%06d", random.nextInt(1000000));
            sessionRepository.saveCode(sessionId, code);

            Session session = sessionRepository.findById(sessionId).orElse(null);
            String courseName = session != null && session.getCourseName() != null
                ? session.getCourseName() : "your class";

            List<String> emails = sessionRepository.findEnrolledStudentEmails(sessionId);
            for (String email : emails) {
                emailService.sendSessionCode(email, courseName, code);
            }

            System.out.println("[SessionService] Session " + sessionId + " activated. Code: " + code + " — emailed " + emails.size() + " students");
        }
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

    /**
     * Returns the session code for a session (lecturer view only).
     */
    public String getSessionCode(int sessionId) throws SQLException {
        return sessionRepository.getCode(sessionId);
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