package com.attendease.repositories;

import com.attendease.config.DatabaseConfig;
import com.attendease.models.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    public Course save(Course course) throws SQLException {
        String sql = "INSERT INTO courses (lecturer_id, course_name, course_code, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, course.getLecturerId());
            stmt.setString(2, course.getCourseName());
            stmt.setString(3, course.getCourseCode());
            stmt.setString(4, course.getDescription());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                course.setId(rs.getInt(1));
            }
        }
        return course;
    }

    public Optional<Course> findById(int id) throws SQLException {
        String sql = "SELECT c.*, u.full_name as lecturer_name FROM courses c JOIN users u ON c.lecturer_id = u.id WHERE c.id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Course> findByLecturerId(int lecturerId) throws SQLException {
        String sql = "SELECT c.*, u.full_name as lecturer_name FROM courses c JOIN users u ON c.lecturer_id = u.id WHERE c.lecturer_id = ? ORDER BY c.created_at DESC";
        List<Course> courses = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, lecturerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                courses.add(mapRow(rs));
            }
        }
        return courses;
    }

    public List<Course> findAllEnrolledByStudent(int studentId) throws SQLException {
        String sql = """
            SELECT c.*, u.full_name as lecturer_name
            FROM courses c
            JOIN users u ON c.lecturer_id = u.id
            JOIN student_courses sc ON c.id = sc.course_id
            WHERE sc.student_id = ?
            ORDER BY c.course_name
            """;
        List<Course> courses = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                courses.add(mapRow(rs));
            }
        }
        return courses;
    }

    public void enrollStudent(int studentId, int courseId) throws SQLException {
        String sql = "INSERT IGNORE INTO student_courses (student_id, course_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            stmt.executeUpdate();
        }
    }

    public List<Course> findAll() throws SQLException {
        String sql = "SELECT c.*, u.full_name as lecturer_name FROM courses c JOIN users u ON c.lecturer_id = u.id ORDER BY c.course_name";
        List<Course> courses = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                courses.add(mapRow(rs));
            }
        }
        return courses;
    }

    public int countEnrolledStudents(int courseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM student_courses WHERE course_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public boolean isEnrolled(int studentId, int courseId) throws SQLException {
        String sql = "SELECT 1 FROM student_courses WHERE student_id = ? AND course_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setId(rs.getInt("id"));
        course.setLecturerId(rs.getInt("lecturer_id"));
        course.setCourseName(rs.getString("course_name"));
        course.setCourseCode(rs.getString("course_code"));
        course.setDescription(rs.getString("description"));
        course.setCreatedAt(rs.getTimestamp("created_at"));
        if (hasColumn(rs, "lecturer_name")) course.setLecturerName(rs.getString("lecturer_name"));
        return course;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
