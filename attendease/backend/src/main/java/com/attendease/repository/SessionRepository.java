package com.attendease.repository;

import com.attendease.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    Optional<Session> findBySessionCode(String sessionCode);
    
    List<Session> findByLecturerId(String lecturerId);
    
    List<Session> findByStatus(Session.Status status);
    
    Long countByLecturerId(String lecturerId);
    
    Long countByStatus(Session.Status status);
    
    Long countByLecturerIdAndStatus(String lecturerId, Session.Status status);
}