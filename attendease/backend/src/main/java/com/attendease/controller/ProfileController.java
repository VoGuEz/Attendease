package com.attendease.controller;

import com.attendease.dto.StudentKpiDto;
import com.attendease.dto.UpdateProfileRequest;
import com.attendease.entity.User;
import com.attendease.entity.Session;
import com.attendease.repository.SessionRepository;
import com.attendease.repository.AttendanceRepository;
import com.attendease.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5500", "http://127.0.0.1:5500"})
public class ProfileController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @GetMapping
    public ResponseEntity<User> getProfile(Authentication authentication) {
        String userId = authentication.getName();
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping
    public ResponseEntity<User> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        User user = authService.getUserById(userId);
        
        if (request.getFullname() != null) {
            user.setFullname(request.getFullname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        
        authService.updateUser(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/kpi/student")
    public ResponseEntity<StudentKpiDto> getStudentKpi(Authentication authentication) {
        String userId = authentication.getName();
        
        System.out.println("📊 Fetching Student KPIs for user: " + userId);
        
        // Get total completed sessions (not just the student's)
        long totalSessions = sessionRepository.countByStatus(Session.Status.completed);
        System.out.println("Total completed sessions: " + totalSessions);
        
        // Get sessions this student attended
        long attendedCount = attendanceRepository.countByStudentId(userId);
        System.out.println("Student attended: " + attendedCount + " sessions");
        
        // Calculate attendance rate
        int attendanceRate = 0;
        if (totalSessions > 0) {
            attendanceRate = (int) Math.round((attendedCount * 100.0) / totalSessions);
        }
        
        System.out.println("Attendance rate: " + attendanceRate + "%");
        
        String status = "Great attendance! Keep it up!";
        if (attendanceRate < 50) {
            status = "Your attendance is low. Please improve!";
        } else if (attendanceRate < 75) {
            status = "You could do better with attendance";
        } else if (attendanceRate < 100) {
            status = "Good attendance! Keep improving!";
        }
        
        StudentKpiDto kpi = new StudentKpiDto(
            (int) attendedCount,
            (int) totalSessions,
            attendanceRate + "%",
            status
        );
        
        System.out.println("KPI Response: " + kpi.getAttended() + " / " + kpi.getTotalSessions() + " = " + kpi.getAttendanceRate());
        
        return ResponseEntity.ok(kpi);
    }
}