package com.attendease.service;

import com.attendease.dto.SessionDTO;
import com.attendease.entity.Course;
import com.attendease.entity.Session;
import com.attendease.repository.AttendanceRepository;
import com.attendease.repository.CourseRepository;
import com.attendease.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;

    public List<SessionDTO> getAllSessions() {
        return sessionRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<SessionDTO> getActiveSessions() {
        return sessionRepository.findActiveAndUpcoming().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<SessionDTO> getSessionsByLecturer(Long lecturerId) {
        return sessionRepository.findByLecturerId(lecturerId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public SessionDTO getSessionById(Long id) {
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        return toDTO(session);
    }

    public SessionDTO createSession(SessionDTO dto) {
        Course course = courseRepository.findById(dto.getCourseId())
            .orElseThrow(() -> new RuntimeException("Course not found"));

        Session session = Session.builder()
            .course(course)
            .sessionDate(dto.getSessionDate())
            .startTime(dto.getStartTime())
            .endTime(dto.getEndTime())
            .status(dto.getStatus() != null
                ? Session.Status.valueOf(dto.getStatus().toUpperCase())
                : Session.Status.UPCOMING)
            .build();

        return toDTO(sessionRepository.save(session));
    }

    public SessionDTO updateSession(Long id, SessionDTO dto) {
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found"));

        if (dto.getSessionDate() != null) session.setSessionDate(dto.getSessionDate());
        if (dto.getStartTime()   != null) session.setStartTime(dto.getStartTime());
        if (dto.getEndTime()     != null) session.setEndTime(dto.getEndTime());
        if (dto.getStatus()      != null) session.setStatus(Session.Status.valueOf(dto.getStatus().toUpperCase()));

        return toDTO(sessionRepository.save(session));
    }

    public SessionDTO updateStatus(Long id, String status) {
        Session session = sessionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus(Session.Status.valueOf(status.toUpperCase()));
        return toDTO(sessionRepository.save(session));
    }

    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }

    private SessionDTO toDTO(Session session) {
        int attendeeCount = attendanceRepository.findBySessionId(session.getId()).size();
        return SessionDTO.builder()
            .id(session.getId())
            .courseId(session.getCourse() != null ? session.getCourse().getId() : null)
            .courseName(session.getCourse() != null ? session.getCourse().getCourseName() : null)
            .courseCode(session.getCourse() != null ? session.getCourse().getCourseCode() : null)
            .sessionDate(session.getSessionDate())
            .startTime(session.getStartTime())
            .endTime(session.getEndTime())
            .status(session.getStatus().name())
            .attendeeCount(attendeeCount)
            .createdAt(session.getCreatedAt())
            .build();
    }
}
