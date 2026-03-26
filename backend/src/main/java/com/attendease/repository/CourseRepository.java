package com.attendease.repository;

import com.attendease.entity.Course;
import com.attendease.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByLecturer(User lecturer);
    List<Course> findByLecturerId(Long lecturerId);
}
