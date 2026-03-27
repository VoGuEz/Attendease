package com.attendease.repositories;

import com.attendease.config.DatabaseConfig;
import com.attendease.models.Attendance;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceRepository {

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    public Attendance save(Attendance attendance) throws SQLException {
        // Try inserting with all columns first, fallback to basic columns if they don't exist
        String sqlWithAllColumns = "INSERT INTO attendance (session_id, student_id, status, submitted_full_name, submitted_index_number, submitted_level, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlBasic = "INSERT INTO attendance (session_id, student_id, status) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlWithAllColumns, Statement.RETURN_GENERATED_KEYS)) {
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
            if (rs.next()) {
                attendance.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                // Columns don't exist yet, use basic insert
                System.err.println("GPS columns not found, using basic insert: " + e.getMessage());
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sqlBasic, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, attendance.getSessionId());
                    stmt.setInt(2, attendance.getStudentId());
                    stmt.setString(3, attendance.getStatus());
                    stmt.executeUpdate();
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        attendance.setId(rs.getInt(1));
                    }
                }
                // Try to update with GPS data if columns eventually exist
                tryUpdateGpsData(attendance);
            } else {
                throw e;
            }
        }
        return attendance;
    }

    public List<Attendance> findBySessionId(int sessionId) throws SQLException {
        String sqlFull = """
            SELECT a.id, a.session_id, a.student_id, a.join_time, a.leave_time, a.status,
                   a.submitted_full_name, a.submitted_index_number, a.submitted_level, a.latitude, a.longitude,
                   COALESCE(a.submitted_full_name, u.full_name) as student_name
            FROM attendance a
            JOIN users u ON a.student_id = u.id
            WHERE a.session_id = ?
            ORDER BY a.join_time
            """;
        String sqlBasic = """
            SELECT a.id, a.session_id, a.student_id, a.join_time, a.leave_time, a.status,
                   u.full_name as student_name
            FROM attendance a
            JOIN users u ON a.student_id = u.id
            WHERE a.session_id = ?
            ORDER BY a.join_time
            """;
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlFull)) {
            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                System.err.println("GPS columns not available, using basic query: " + e.getMessage());
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sqlBasic)) {
                    stmt.setInt(1, sessionId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            } else {
                throw e;
            }
        }
        return list;
    }

    public List<Attendance> findByStudentId(int studentId) throws SQLException {
        String sqlFull = """
            SELECT a.id, a.session_id, a.student_id, a.join_time, a.leave_time, a.status,
                   a.submitted_full_name, a.submitted_index_number, a.submitted_level, 
                   a.latitude, a.longitude
            FROM attendance a 
            WHERE a.student_id = ? 
            ORDER BY a.join_time DESC
            """;
        String sqlBasic = """
            SELECT a.id, a.session_id, a.student_id, a.join_time, a.leave_time, a.status
            FROM attendance a 
            WHERE a.student_id = ? 
            ORDER BY a.join_time DESC
            """;
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlFull)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown column")) {
                System.err.println("GPS columns not available, using basic query: " + e.getMessage());
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sqlBasic)) {
                    stmt.setInt(1, studentId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            } else {
                throw e;
            }
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
        Map<String, Object> stats = new HashMap<>();

        String totalSessionsSql = """
            SELECT COUNT(DISTINCT s.id) as total
            FROM sessions s
            JOIN courses c ON s.course_id = c.id
            JOIN student_courses sc ON c.id = sc.course_id
            WHERE sc.student_id = ? AND s.status IN ('active','completed')
            """;

        String attendedSql = """
            SELECT COUNT(*) as attended
            FROM attendance
            WHERE student_id = ? AND status IN ('present','late')
            """;

        int totalSessions = 0;
        int attendedSessions = 0;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(totalSessionsSql)) {
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

        stats.put("totalSessions", totalSessions);
        stats.put("attendedSessions", attendedSessions);
        stats.put("percentage", Math.round(percentage * 10.0) / 10.0);
        return stats;
    }

    private void tryUpdateGpsData(Attendance attendance) {
        if (attendance.getId() <= 0 || (attendance.getSubmittedFullName() == null && attendance.getLatitude() == null)) {
            return;
        }
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE attendance SET submitted_full_name = ?, submitted_index_number = ?, submitted_level = ?, latitude = ?, longitude = ? WHERE id = ?")) {
            stmt.setString(1, attendance.getSubmittedFullName());
            stmt.setString(2, attendance.getSubmittedIndexNumber());
            stmt.setString(3, attendance.getSubmittedLevel());
            stmt.setObject(4, attendance.getLatitude());
            stmt.setObject(5, attendance.getLongitude());
            stmt.setInt(6, attendance.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Columns might not exist yet, silently ignore
            System.err.println("Could not update GPS data (columns may not exist yet): " + e.getMessage());
        }
    }

    private Attendance mapRow(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setSessionId(rs.getInt("session_id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setJoinTime(rs.getTimestamp("join_time"));
        a.setLeaveTime(rs.getTimestamp("leave_time"));
        a.setStatus(rs.getString("status"));
        if (hasColumn(rs, "student_name")) a.setStudentName(rs.getString("student_name"));
        if (hasColumn(rs, "submitted_full_name")) a.setSubmittedFullName(rs.getString("submitted_full_name"));
        if (hasColumn(rs, "submitted_index_number")) a.setSubmittedIndexNumber(rs.getString("submitted_index_number"));
        if (hasColumn(rs, "submitted_level")) a.setSubmittedLevel(rs.getString("submitted_level"));
        if (hasColumn(rs, "latitude")) a.setLatitude((Double) rs.getObject("latitude"));
        if (hasColumn(rs, "longitude")) a.setLongitude((Double) rs.getObject("longitude"));
        return a;
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
