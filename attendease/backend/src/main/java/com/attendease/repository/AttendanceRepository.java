package com.attendease.repository;

import com.attendease.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {
    List<Attendance> findBySessionId(String sessionId);
    
    List<Attendance> findByStudentId(String studentId);
    
    Long countByStudentId(String studentId);
    
    boolean existsBySessionIdAndStudentId(String sessionId, String studentId);
}