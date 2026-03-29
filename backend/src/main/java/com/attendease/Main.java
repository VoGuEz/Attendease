package com.attendease;

import com.attendease.handlers.AttendanceHandler;
import com.attendease.handlers.AuthHandler;
import com.attendease.handlers.CourseHandler;
import com.attendease.handlers.SessionHandler;
import com.attendease.repositories.AttendanceRepository;
import com.attendease.repositories.CourseRepository;
import com.attendease.repositories.SessionRepository;
import com.attendease.repositories.UserRepository;
import com.attendease.services.AttendanceService;
import com.attendease.services.AuthService;
import com.attendease.services.CourseService;
import com.attendease.services.EmailService;
import com.attendease.services.SessionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        String dbHost     = System.getenv("DB_HOST");
        String dbPort     = System.getenv().getOrDefault("DB_PORT", "3306");
        String dbName     = System.getenv("DB_NAME");
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbHost == null || dbName == null || dbUser == null) {
            System.err.println("FATAL: DB_HOST, DB_NAME, or DB_USER not set!");
            System.exit(1);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=true&serverTimezone=UTC");
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setKeepaliveTime(60000);
        config.setConnectionTestQuery("SELECT 1");

        HikariDataSource dataSource = new HikariDataSource(config);

        try (Connection testConn = dataSource.getConnection()) {
            System.out.println("[Main] Database connected successfully!");
        }

        // Repositories
        UserRepository       userRepository       = new UserRepository(dataSource);
        CourseRepository     courseRepository     = new CourseRepository(dataSource);
        SessionRepository    sessionRepository    = new SessionRepository(dataSource);
        AttendanceRepository attendanceRepository = new AttendanceRepository(dataSource);

        // EmailService shared across AuthService and SessionService
        EmailService      emailService      = new EmailService();
        AuthService       authService       = new AuthService(userRepository, emailService);
        CourseService     courseService     = new CourseService(courseRepository);
        SessionService    sessionService    = new SessionService(sessionRepository, emailService);
        AttendanceService attendanceService = new AttendanceService(attendanceRepository, sessionRepository);

        // Handlers
        AuthHandler       authHandler       = new AuthHandler(authService, userRepository);
        CourseHandler     courseHandler     = new CourseHandler(courseService, sessionService);
        SessionHandler    sessionHandler    = new SessionHandler(sessionService);
        AttendanceHandler attendanceHandler = new AttendanceHandler(attendanceService);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/auth/register",      e -> { addCorsHeaders(e); if (!handlePreflight(e)) authHandler.handleRegister(e); });
        server.createContext("/api/auth/login",         e -> { addCorsHeaders(e); if (!handlePreflight(e)) authHandler.handleLogin(e); });
        server.createContext("/api/auth/update-name",   e -> { addCorsHeaders(e); if (!handlePreflight(e)) authHandler.handleUpdateName(e); });
        server.createContext("/api/auth/request-reset", e -> { addCorsHeaders(e); if (!handlePreflight(e)) authHandler.handleRequestReset(e); });
        server.createContext("/api/auth/reset-password",e -> { addCorsHeaders(e); if (!handlePreflight(e)) authHandler.handleResetPassword(e); });
        server.createContext("/api/students",           e -> { addCorsHeaders(e); if (!handlePreflight(e)) authHandler.handleGetStudents(e); });
        server.createContext("/api/courses",            e -> { addCorsHeaders(e); if (!handlePreflight(e)) courseHandler.handle(e); });
        server.createContext("/api/sessions",           e -> { addCorsHeaders(e); if (!handlePreflight(e)) sessionHandler.handle(e); });
        server.createContext("/api/attendance",         e -> { addCorsHeaders(e); if (!handlePreflight(e)) attendanceHandler.handle(e); });

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("[Main] AttendEase server started on port " + port);
    }

    static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}