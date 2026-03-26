package com.attendease.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    // DB connection settings can be overridden via environment variables for production.
    // Defaults match a standard XAMPP local setup (root user, no password).
    private static final String HOST     = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final int    PORT     = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "3306"));
    private static final String DATABASE = System.getenv().getOrDefault("DB_NAME", "attendease");
    private static final String USER     = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    private static final String URL = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            HOST, PORT, DATABASE
    );

    private static DatabaseConfig instance;

    private DatabaseConfig() {
        initializeDatabase();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private void initializeDatabase() {
        String createDb = "CREATE DATABASE IF NOT EXISTS " + DATABASE;
        String urlNoDb = String.format(
                "jdbc:mysql://%s:%d?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                HOST, PORT
        );
        try (Connection conn = DriverManager.getConnection(urlNoDb, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createDb);
        } catch (SQLException e) {
            System.err.println("Could not create database: " + e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
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
