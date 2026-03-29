package com.attendease.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class EmailService {
    private final String apiKey;
    private final String fromEmail;

    public EmailService() {
        this.apiKey = System.getenv("BREVO_API_KEY");
        this.fromEmail = System.getenv("SMTP_FROM");
    }

    public void sendResetCode(String targetEmail, String token) {
        String subject = "AttendEase - Password Reset Code";
        String body = "Your password reset code is: " + token + "\n\nThis code is valid for your current session only.";
        sendEmail(targetEmail, subject, body);
    }

    /**
     * Sends the session join code to a student when their lecturer starts a session.
     */
    public void sendSessionCode(String targetEmail, String courseName, String code) {
        String subject = "AttendEase - Session Code for " + courseName;
        String body = "Your lecturer has started a session for " + courseName + ".\n\n"
                    + "Your session code is: " + code + "\n\n"
                    + "Enter this code in the AttendEase app to join the session and mark your attendance.\n"
                    + "This code is only valid while the session is active.";
        sendEmail(targetEmail, subject, body);
    }

    private void sendEmail(String targetEmail, String subject, String textContent) {
        new Thread(() -> {
            try {
                URL url = URI.create("https://api.brevo.com/v3/smtp/email").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("api-key", apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String safeContent = textContent
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

                String jsonBody = "{"
                    + "\"sender\":{\"email\":\"" + fromEmail + "\",\"name\":\"AttendEase\"},"
                    + "\"to\":[{\"email\":\"" + targetEmail + "\"}],"
                    + "\"subject\":\"" + subject + "\","
                    + "\"textContent\":\"" + safeContent + "\""
                    + "}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    System.out.println("[EmailService] Email sent successfully to: " + targetEmail);
                } else {
                    try (InputStream es = conn.getErrorStream()) {
                        if (es != null) {
                            try (Scanner s = new Scanner(es, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                                String errorResponse = s.hasNext() ? s.next() : "No error message provided";
                                System.err.println("[EmailService] Brevo API Error: " + errorResponse);
                            }
                        }
                    }
                    System.err.println("[EmailService] Failed with Response Code: " + responseCode);
                }
                conn.disconnect();

            } catch (Exception e) {
                System.err.println("[EmailService] Critical API Failure: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}