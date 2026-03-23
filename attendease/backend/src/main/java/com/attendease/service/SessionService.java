package com.attendease.service;

import com.attendease.entity.Session;
import com.attendease.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    public Session createSession(String title, int durationMinutes, String lecturerId) {
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setTitle(title);
        session.setSessionCode(generateSessionCode());
        session.setLecturerId(lecturerId);
        session.setDurationMinutes(durationMinutes);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes));
        session.setStatus(Session.Status.active);
        session.setCreatedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    public Session getSessionByCode(String code) {
        Session session = sessionRepository.findBySessionCode(code)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setStatus(Session.Status.completed);
            sessionRepository.save(session);
            throw new RuntimeException("Session has expired");
        }

        return session;
    }

    public Session getSessionById(String sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    public List<Session> getSessionsByLecturer(String lecturerId) {
        return sessionRepository.findByLecturerId(lecturerId);
    }

    public void endSession(String sessionId) {
        Session session = getSessionById(sessionId);
        session.setStatus(Session.Status.completed);
        sessionRepository.save(session);
    }

    public Long getTotalSessions(String lecturerId) {
        return sessionRepository.countByLecturerId(lecturerId);
    }

    public Long getActiveSessions(String lecturerId) {
        return sessionRepository.countByLecturerIdAndStatus(lecturerId, Session.Status.active);
    }

    public Long getCompletedSessions(String lecturerId) {
        return sessionRepository.countByLecturerIdAndStatus(lecturerId, Session.Status.completed);
    }

    public Map<String, Long> getLecturerKpis(String lecturerId) {
        Map<String, Long> kpis = new HashMap<>();
        kpis.put("totalSessions", getTotalSessions(lecturerId));
        kpis.put("activeSessions", getActiveSessions(lecturerId));
        kpis.put("completedSessions", getCompletedSessions(lecturerId));
        return kpis;
    }

    @Scheduled(fixedRate = 60000)
    public void autoEndExpiredSessions() {
        List<Session> activeSessions = sessionRepository.findByStatus(Session.Status.active);
        for (Session session : activeSessions) {
            if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                session.setStatus(Session.Status.completed);
                sessionRepository.save(session);
            }
        }
    }

    private String generateSessionCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}