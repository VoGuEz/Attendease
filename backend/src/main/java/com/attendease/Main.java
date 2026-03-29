package com.attendease;

import com.attendease.handlers.AttendanceHandler;
import com.attendease.handlers.AuthHandler;
import com.attendease.handlers.CourseHandler;
import com.attendease.handlers.SessionHandler;
import com.attendease.repositories.UserRepository;
import com.attendease.services.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        // 1. Get the Port from Railway (Default to 8080 if local)
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // 2. Safely get the Database URL
        String dbUrl = System.getenv("DATABASE_URL");
        
        // CRITICAL FIX: Prevent the null crash and handle the jdbc: prefix
        if (dbUrl == null || dbUrl.isEmpty()) {
            System.err.println("FATAL ERROR: DATABASE_URL is not set in Railway variables!");
            System.exit(1); 
        }

        if (!dbUrl.startsWith("jdbc:")) {
            dbUrl = "jdbc:" + dbUrl;
        }

        // 3. Load the MySQL Driver (Prevents 'No suitable driver' error)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found in classpath!");
            e.printStackTrace();
        }

        // 4. Establish Connection
        Connection conn = DriverManager.getConnection(dbUrl);
        System.out.println("Successfully connected to the database!");

        // 5. Initialize Repositories and Services
        UserRepository userRepository = new UserRepository(conn);
        // Assuming AuthService needs the repo; passing null for other params as per your original code
        AuthService authService = new AuthService(userRepository, null, null);

        AuthHandler authHandler = new AuthHandler(authService, userRepository);
        CourseHandler courseHandler = new CourseHandler();
        SessionHandler sessionHandler = new SessionHandler();
        AttendanceHandler attendanceHandler = new AttendanceHandler();

        // 6. Start the Server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // --- AUTH ROUTES ---
        server.createContext("/api/auth/register", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            authHandler.handleRegister(exchange);
        });

        server.createContext("/api/auth/login", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            authHandler.handleLogin(exchange);
        });

        server.createContext("/api/auth/update-name", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            authHandler.handleUpdateName(exchange);
        });

        server.createContext("/api/auth/request-reset", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            authHandler.handleRequestReset(exchange);
        });

        server.createContext("/api/auth/reset-password", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            authHandler.handleResetPassword(exchange);
        });

        // --- DATA ROUTES ---
        server.createContext("/api/students", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            authHandler.handleGetStudents(exchange);
        });

        server.createContext("/api/courses", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            courseHandler.handle(exchange);
        });

        server.createContext("/api/sessions", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            sessionHandler.handle(exchange);
        });

        server.createContext("/api/attendance", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            attendanceHandler.handle(exchange);
        });

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("AttendEase server started on port " + port);
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