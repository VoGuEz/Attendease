package com.attendease.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    private static final String RAW_DB_URL = firstNonEmpty(
            System.getenv("DB_URL"),
            System.getenv("DATABASE_URL"),
            System.getenv("MYSQL_URL")
    );

    private static final String HOST = getEnv("DB_HOST", "MYSQLHOST", "localhost");
    private static final int PORT = Integer.parseInt(getEnv("DB_PORT", "MYSQLPORT", "3306"));
    private static final String DATABASE = getEnv("DB_NAME", "MYSQLDATABASE", "attendease");
    private static final String USER = getEnv("DB_USER", "MYSQLUSER", "root");
    private static final String PASSWORD = getEnv("DB_PASSWORD", "MYSQLPASSWORD", "");

    private static final DbConnectionInfo DB_INFO = resolveConnectionInfo();

    /** Returns the value of {@code primary} env var, falling back to {@code fallback}, then {@code defaultValue}. */
    private static String getEnv(String primary, String fallback, String defaultValue) {
        String value = System.getenv(primary);
        if (value != null && !value.isEmpty()) return value;
        value = System.getenv(fallback);
        if (value != null && !value.isEmpty()) return value;
        return defaultValue;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static DbConnectionInfo resolveConnectionInfo() {
        if (RAW_DB_URL == null) {
            String url = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    HOST, PORT, DATABASE
            );
            String urlNoDb = String.format(
                    "jdbc:mysql://%s:%d?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    HOST, PORT
            );
            return new DbConnectionInfo(url, urlNoDb, USER, PASSWORD, DATABASE, HOST, PORT, "DB_* or MYSQL* vars");
        }

        try {
            String normalized = RAW_DB_URL.startsWith("jdbc:") ? RAW_DB_URL.substring(5) : RAW_DB_URL;
            URI uri = new URI(normalized);

            String host = uri.getHost() != null ? uri.getHost() : HOST;
            int port = uri.getPort() > 0 ? uri.getPort() : PORT;

            String path = uri.getPath();
            String database = (path != null && path.length() > 1) ? path.substring(1) : DATABASE;

            String resolvedUser = USER;
            String resolvedPassword = PASSWORD;
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                resolvedUser = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (parts.length > 1) {
                    resolvedPassword = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            String query = uri.getRawQuery();
            String suffix = (query == null || query.isBlank())
                    ? "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                    : "?" + query;

            String url = String.format("jdbc:mysql://%s:%d/%s%s", host, port, database, suffix);
            String urlNoDb = String.format("jdbc:mysql://%s:%d%s", host, port, suffix);
            return new DbConnectionInfo(url, urlNoDb, resolvedUser, resolvedPassword, database, host, port,
                    "DB_URL / DATABASE_URL / MYSQL_URL");
        } catch (URISyntaxException e) {
            System.err.println("Invalid database URL format, falling back to DB_* variables: " + e.getMessage());
            String url = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    HOST, PORT, DATABASE
            );
            String urlNoDb = String.format(
                    "jdbc:mysql://%s:%d?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    HOST, PORT
            );
            return new DbConnectionInfo(url, urlNoDb, USER, PASSWORD, DATABASE, HOST, PORT, "DB_* fallback");
        }
    }

    private record DbConnectionInfo(
            String url,
            String urlNoDb,
            String user,
            String password,
            String database,
            String host,
            int port,
            String source
    ) {}

    private static DatabaseConfig instance;

    private DatabaseConfig() {
        System.out.printf("DB target: host=%s port=%d db=%s source=%s%n",
                DB_INFO.host(), DB_INFO.port(), DB_INFO.database(), DB_INFO.source());
        initializeDatabase();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_INFO.url(), DB_INFO.user(), DB_INFO.password());
    }

    private void initializeDatabase() {
        String createDb = "CREATE DATABASE IF NOT EXISTS " + DB_INFO.database();
        try (Connection conn = DriverManager.getConnection(DB_INFO.urlNoDb(), DB_INFO.user(), DB_INFO.password());
             Statement stmt = conn.createStatement()) {
            stmt.execute(createDb);
        } catch (SQLException e) {
            System.err.println("Could not create database: " + e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(DB_INFO.url(), DB_INFO.user(), DB_INFO.password());
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name VARCHAR(255) NOT NULL,
                    role ENUM('student','lecturer') NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS courses (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    lecturer_id INT NOT NULL,
                    course_name VARCHAR(255) NOT NULL,
                    course_code VARCHAR(50) NOT NULL UNIQUE,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (lecturer_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    course_id INT NOT NULL,
                    session_date DATE NOT NULL,
                    start_time TIME NOT NULL,
                    end_time TIME NOT NULL,
                    status ENUM('upcoming','active','completed') NOT NULL DEFAULT 'upcoming',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    session_id INT NOT NULL,
                    student_id INT NOT NULL,
                    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    leave_time TIMESTAMP NULL,
                    status ENUM('present','absent','late') NOT NULL DEFAULT 'present',
                    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    UNIQUE KEY unique_attendance (session_id, student_id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS student_courses (
                    student_id INT NOT NULL,
                    course_id INT NOT NULL,
                    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (student_id, course_id),
                    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                )
                """);

            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }
}
