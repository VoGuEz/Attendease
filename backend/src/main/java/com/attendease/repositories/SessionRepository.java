package com.attendease.repositories;

import com.attendease.config.DatabaseConfig;
import com.attendease.models.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SessionRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    public Session save(Session session) throws SQLException {
        String sql = "INSERT INTO sessions (course_id, session_date, start_time, end_time, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, session.getCourseId());
            stmt.setDate(2, session.getSessionDate());
            stmt.setTime(3, session.getStartTime());
            stmt.setTime(4, session.getEndTime());
            stmt.setString(5, session.getStatus() != null ? session.getStatus() : "upcoming");
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                session.setId(rs.getInt(1));
            }
        }
        return session;
    }

    public Optional<Session> findById(int id) throws SQLException {
        String sql = """
            SELECT s.*, c.course_name FROM sessions s
            JOIN courses c ON s.course_id = c.id
            WHERE s.id = ?
            """;
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

    public List<Session> findByCourseId(int courseId) throws SQLException {
        String sql = """
            SELECT s.*, c.course_name FROM sessions s
            JOIN courses c ON s.course_id = c.id
            WHERE s.course_id = ?
            ORDER BY s.session_date DESC, s.start_time DESC
            """;
        List<Session> sessions = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(mapRow(rs));
            }
        }
        return sessions;
    }

    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE sessions SET status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public List<Session> findActiveSessions() throws SQLException {
        String sql = """
            SELECT s.*, c.course_name FROM sessions s
            JOIN courses c ON s.course_id = c.id
            WHERE s.status = 'active'
            ORDER BY s.session_date DESC
            """;
        List<Session> sessions = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(mapRow(rs));
            }
        }
        return sessions;
    }

    public List<Session> findActiveSessionsForStudent(int studentId) throws SQLException {
        String sql = """
            SELECT s.*, c.course_name FROM sessions s
            JOIN courses c ON s.course_id = c.id
            JOIN student_courses sc ON c.id = sc.course_id
            WHERE s.status = 'active' AND sc.student_id = ?
            ORDER BY s.session_date DESC
            """;
        List<Session> sessions = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(mapRow(rs));
            }
        }
        return sessions;
    }

    private Session mapRow(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setId(rs.getInt("id"));
        session.setCourseId(rs.getInt("course_id"));
        session.setSessionDate(rs.getDate("session_date"));
        session.setStartTime(rs.getTime("start_time"));
        session.setEndTime(rs.getTime("end_time"));
        session.setStatus(rs.getString("status"));
        session.setCreatedAt(rs.getTimestamp("created_at"));
        try { session.setCourseName(rs.getString("course_name")); } catch (SQLException ignored) {}
        return session;
    }
}
