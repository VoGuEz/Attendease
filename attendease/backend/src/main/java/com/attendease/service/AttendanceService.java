package com.attendease.service;

import com.attendease.entity.Attendance;
import com.attendease.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SessionService sessionService;

    public Attendance joinSession(String sessionCode, String fullname, String indexNumber, 
                                 int level, Double latitude, Double longitude) {
        sessionService.getSessionByCode(sessionCode);

        Attendance attendance = new Attendance();
        attendance.setId(UUID.randomUUID().toString());
        attendance.setSessionId(sessionCode);
        attendance.setFullname(fullname);
        attendance.setIndexNumber(indexNumber);
        attendance.setLevel(level);
        attendance.setLatitude(latitude);
        attendance.setLongitude(longitude);
        attendance.setJoinedAt(LocalDateTime.now());
        attendance.setCreatedAt(LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getAttendanceBySession(String sessionId) {
        return attendanceRepository.findBySessionId(sessionId);
    }

    public Long getStudentAttendanceCount(String studentId) {
        return attendanceRepository.findBySessionId(studentId).stream().count();
    }
}