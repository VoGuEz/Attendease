package com.attendease.services;

import com.attendease.models.Course;
import com.attendease.repositories.CourseRepository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseService {

    private final CourseRepository courseRepository = new CourseRepository();

    public Course createCourse(int lecturerId, String name, String code, String description) throws SQLException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Course name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Course code is required");

        Course course = new Course(lecturerId, name.trim(), code.trim().toUpperCase(), description != null ? description.trim() : "");
        return courseRepository.save(course);
    }

    public List<Course> getCoursesForLecturer(int lecturerId) throws SQLException {
        List<Course> courses = courseRepository.findByLecturerId(lecturerId);
        for (Course c : courses) {
            courseRepository.countEnrolledStudents(c.getId()); // call for side effects if needed
            c.setDescription(c.getDescription() != null ? c.getDescription() : "");
        }
        return courses;
    }

    public Map<String, Object> getCoursesForStudent(int studentId) throws SQLException {
        List<Course> enrolled = courseRepository.findAllEnrolledByStudent(studentId);
        List<Course> all = courseRepository.findAll();

        all.removeIf(c -> enrolled.stream().anyMatch(e -> e.getId() == c.getId()));

        Map<String, Object> result = new HashMap<>();
        result.put("enrolled", enrolled);
        result.put("available", all);
        return result;
    }

    public void enrollStudent(int studentId, int courseId) throws SQLException {
        courseRepository.enrollStudent(studentId, courseId);
    }

    public Course getCourseById(int id) throws SQLException {
        return courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
    }

    public int getEnrolledCount(int courseId) throws SQLException {
        return courseRepository.countEnrolledStudents(courseId);
    }

    public Course updateCourse(int courseId, int lecturerId, String name, String code, String description) throws SQLException {
        Course existing = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (existing.getLecturerId() != lecturerId) throw new IllegalArgumentException("You can only edit your own courses");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Course name is required");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Course code is required");
        courseRepository.update(courseId, name.trim(), code.trim().toUpperCase(), description != null ? description.trim() : "");
        return courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found"));
    }

    public void deleteCourse(int courseId, int lecturerId) throws SQLException {
        Course existing = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (existing.getLecturerId() != lecturerId) throw new IllegalArgumentException("You can only delete your own courses");
        courseRepository.delete(courseId);
    }
}
