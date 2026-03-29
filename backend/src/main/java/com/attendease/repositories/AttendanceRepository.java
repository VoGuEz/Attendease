package com.attendease.repositories;

import com.attendease.models.Attendance;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceRepository {
    private final HikariDataSource dataSource;

    public AttendanceRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public Attendance save(Attendance attendance) throws SQLException {
        String sql = "INSERT INTO attendance (session_id, student_id, status, submitted_full_name, submitted_index_number, submitted_level, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, attendance.getSessionId());
            stmt.setInt(2, attendance.getStudentId());
            stmt.setString(3, attendance.getStatus());
            stmt.setString(4, attendance.getSubmittedFullName());
            stmt.setString(5, attendance.getSubmittedIndexNumber());
            stmt.setString(6, attendance.getSubmittedLevel());
            stmt.setObject(7, attendance.getLatitude());
            stmt.setObject(8, attendance.getLongitude());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) attendance.setId(rs.getInt(1));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                String basicSql = "INSERT INTO attendance (session_id, student_id, status) VALUES (?, ?, ?)";
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(basicSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, attendance.getSessionId());
                    stmt.setInt(2, attendance.getStudentId());
                    stmt.setString(3, attendance.getStatus());
                    stmt.executeUpdate();
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) attendance.setId(rs.getInt(1));
                }
            } else {
                throw e;
            }
        }
        return attendance;
    }

    public List<Attendance> findBySessionId(int sessionId) throws SQLException {
        String sql = """
            SELECT a.*, COALESCE(a.submitted_full_name, u.full_name) as student_name
            FROM attendance a
            JOIN users u ON a.student_id = u.id
            WHERE a.session_id = ?
            ORDER BY a.join_time
            """;
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Attendance> findByStudentId(int studentId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE student_id = ? ORDER BY join_time DESC";
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean existsBySessionAndStudent(int sessionId, int studentId) throws SQLException {
        String sql = "SELECT 1 FROM attendance WHERE session_id = ? AND student_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.setInt(2, studentId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public Map<String, Object> getStudentStats(int studentId) throws SQLException {
        int totalSessions = 0, attendedSessions = 0;
        String totalSql = """
            SELECT COUNT(DISTINCT s.id) as total
            FROM sessions s
            JOIN courses c ON s.course_id = c.id
            JOIN student_courses sc ON c.id = sc.course_id
            WHERE sc.student_id = ? AND s.status IN ('active','completed')
            """;
        String attendedSql = "SELECT COUNT(*) as attended FROM attendance WHERE student_id = ? AND status IN ('present','late')";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(totalSql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) totalSessions = rs.getInt("total");
        }
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(attendedSql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) attendedSessions = rs.getInt("attended");
        }

        double percentage = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100.0 : 0.0;
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", totalSessions);
        stats.put("attendedSessions", attendedSessions);
        stats.put("percentage", Math.round(percentage * 10.0) / 10.0);
        return stats;
    }

    private Attendance mapRow(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setSessionId(rs.getInt("session_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setJoinTime(rs.getTimestamp("join_time"));
        a.setLeaveTime(rs.getTimestamp("leave_time"));
        a.setStatus(rs.getString("status"));
        try { a.setStudentName(rs.getString("student_name")); } catch (SQLException ignored) {}
        try { a.setSubmittedFullName(rs.getString("submitted_full_name")); } catch (SQLException ignored) {}
        try { a.setSubmittedIndexNumber(rs.getString("submitted_index_number")); } catch (SQLException ignored) {}
        try { a.setSubmittedLevel(rs.getString("submitted_level")); } catch (SQLException ignored) {}
        try { a.setLatitude((Double) rs.getObject("latitude")); } catch (SQLException ignored) {}
        try { a.setLongitude((Double) rs.getObject("longitude")); } catch (SQLException ignored) {}
        return a;
    }
}