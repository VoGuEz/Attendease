package com.attendease;

import com.attendease.handlers.AttendanceHandler;
import com.attendease.handlers.AuthHandler;
import com.attendease.handlers.CourseHandler;
import com.attendease.handlers.SessionHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        AuthHandler authHandler = new AuthHandler();
        CourseHandler courseHandler = new CourseHandler();
        SessionHandler sessionHandler = new SessionHandler();
        AttendanceHandler attendanceHandler = new AttendanceHandler();

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

        server.createContext("/api/auth/profile", exchange -> {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) return;
            authHandler.handleUpdateProfile(exchange);
        });

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
        System.out.println("AttendEase server started on port 8080");
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
