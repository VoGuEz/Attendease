package com.attendease.service;

import com.attendease.dto.AttendanceDTO;
import com.attendease.dto.StudentStatsDTO;
import com.attendease.entity.Attendance;
import com.attendease.entity.Session;
import com.attendease.entity.User;
import com.attendease.repository.AttendanceRepository;
import com.attendease.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final UserService userService;

    public AttendanceDTO joinSession(Long sessionId, String studentEmail, Double lat, Double lng) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        User student = userService.findByEmail(studentEmail)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if already joined
        attendanceRepository.findBySessionIdAndStudentId(sessionId, student.getId())
            .ifPresent(a -> { throw new RuntimeException("Already joined this session"); });

        // Determine if LATE (after startTime)
        Attendance.Status status = Attendance.Status.PRESENT;
        if (session.getStartTime() != null && LocalTime.now().isAfter(session.getStartTime().plusMinutes(10))) {
            status = Attendance.Status.LATE;
        }

        Attendance attendance = Attendance.builder()
            .session(session)
            .student(student)
            .joinTime(LocalDateTime.now())
            .status(status)
            .locationLatitude(lat)
            .locationLongitude(lng)
            .build();

        return toDTO(attendanceRepository.save(attendance));
    }

    public AttendanceDTO leaveSession(Long sessionId, String studentEmail) {
        User student = userService.findByEmail(studentEmail)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        Attendance attendance = attendanceRepository.findBySessionIdAndStudentId(sessionId, student.getId())
            .orElseThrow(() -> new RuntimeException("Attendance record not found"));

        attendance.setLeaveTime(LocalDateTime.now());
        return toDTO(attendanceRepository.save(attendance));
    }

    public List<AttendanceDTO> getAttendanceBySession(Long sessionId) {
        return attendanceRepository.findBySessionId(sessionId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<AttendanceDTO> getAttendanceByStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public StudentStatsDTO getStudentStats(Long studentId) {
        long attended = attendanceRepository.countAttendedByStudentId(studentId);
        long total    = attendanceRepository.countTotalByStudentId(studentId);
        int rate = total > 0 ? (int) Math.round((attended * 100.0) / total) : 0;
        return new StudentStatsDTO(attended, total, rate);
    }

    private AttendanceDTO toDTO(Attendance a) {
        return AttendanceDTO.builder()
            .id(a.getId())
            .sessionId(a.getSession() != null ? a.getSession().getId() : null)
            .studentId(a.getStudent() != null ? a.getStudent().getId() : null)
            .studentName(a.getStudent() != null ? a.getStudent().getFullName() : null)
            .email(a.getStudent() != null ? a.getStudent().getEmail() : null)
            .joinTime(a.getJoinTime())
            .leaveTime(a.getLeaveTime())
            .status(a.getStatus().name())
            .locationLatitude(a.getLocationLatitude())
            .locationLongitude(a.getLocationLongitude())
            .build();
    }
}
