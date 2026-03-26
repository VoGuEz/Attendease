package com.attendease.repository;

import com.attendease.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByStatus(Session.Status status);

    @Query("SELECT s FROM Session s WHERE s.course.lecturer.id = :lecturerId")
    List<Session> findByLecturerId(@Param("lecturerId") Long lecturerId);

    @Query("SELECT s FROM Session s WHERE s.course.id = :courseId")
    List<Session> findByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT s FROM Session s WHERE s.status IN ('ACTIVE', 'UPCOMING') ORDER BY s.sessionDate, s.startTime")
    List<Session> findActiveAndUpcoming();
}
