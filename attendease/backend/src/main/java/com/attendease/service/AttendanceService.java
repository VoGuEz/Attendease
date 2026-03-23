package com.attendease.service;

import com.attendease.entity.Attendance;
import com.attendease.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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
        attendance.setLatitude(latitude.floatValue());
        attendance.setLongitude(longitude.floatValue());
        attendance.setJoinedAt(LocalDateTime.now());
        attendance.setCreatedAt(LocalDateTime.now());

        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getAttendanceBySession(String sessionId) {
        return attendanceRepository.findBySessionId(sessionId);
    }

    public List<Attendance> getUserAttendance(String userId) {
        return attendanceRepository.findByIndexNumber(userId);
    }

    public String exportAttendanceCSV(String sessionId) {
        List<Attendance> records = attendanceRepository.findBySessionId(sessionId);
        StringBuilder csv = new StringBuilder();
        csv.append("Full Name,Index Number,Level,Latitude,Longitude,Joined At\n");
        for (Attendance a : records) {
            csv.append(escape(a.getFullname())).append(",")
               .append(escape(a.getIndexNumber())).append(",")
               .append(a.getLevel()).append(",")
               .append(a.getLatitude()).append(",")
               .append(a.getLongitude()).append(",")
               .append(a.getJoinedAt()).append("\n");
        }
        return csv.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public Long getStudentAttendanceCount(String studentId) {
        return (long) attendanceRepository.findByIndexNumber(studentId).size();
    }
}