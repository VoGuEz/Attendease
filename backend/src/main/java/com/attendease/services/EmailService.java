package com.attendease.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EmailService {

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final boolean enabled;
    private final HttpClient httpClient;

    public EmailService() {
        this.apiKey = System.getenv("BREVO_API_KEY");
        this.fromEmail = envOrDefault("SMTP_FROM", envOrDefault("SMTP_USER", "noreply@attendease.app"));
        this.fromName = "AttendEase";
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        if (!enabled) {
            System.out.println("[EmailService] BREVO_API_KEY not set. Email sending disabled.");
        } else {
            System.out.println("[EmailService] Brevo email API configured. Sender: " + fromEmail);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void sendResetCode(String toEmail, String resetCode, String userName) {
        if (!enabled) {
            System.out.println("[EmailService] Skipping email (not configured). Reset code for " + toEmail + ": " + resetCode);
            return;
        }

        String subject = "AttendEase - Password Reset Code";
        String htmlBody = buildResetEmailHtml(resetCode, userName);

        try {
            sendViaBrevo(toEmail, subject, htmlBody);
            System.out.println("[EmailService] Reset code sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email to " + toEmail + ": " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private void sendViaBrevo(String to, String subject, String htmlBody) throws Exception {
        String jsonBody = "{" +
            "\"sender\":{\"name\":\"" + escapeJson(fromName) + "\",\"email\":\"" + escapeJson(fromEmail) + "\"}," +
            "\"to\":[{\"email\":\"" + escapeJson(to) + "\"}]," +
            "\"subject\":\"" + escapeJson(subject) + "\"," +
            "\"htmlContent\":" + toJsonString(htmlBody) +
            "}";

        System.out.println("[EmailService] Sending email via Brevo API to " + to);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[EmailService] Brevo response: " + response.statusCode() + " " + response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Email API error (" + response.statusCode() + "): " + response.body());
        }
    }

    private String buildResetEmailHtml(String resetCode, String userName) {
        String name = (userName != null && !userName.isBlank()) ? userName : "there";
        return "<!DOCTYPE html>"
             + "<html><head><meta charset='UTF-8'></head>"
             + "<body style='margin:0;padding:0;background-color:#0f172a;font-family:Arial,sans-serif;'>"
             + "<div style='max-width:480px;margin:40px auto;background:#1e293b;border-radius:16px;padding:40px;'>"
             + "  <div style='text-align:center;margin-bottom:24px;'>"
             + "    <h1 style='color:#6366f1;font-size:28px;margin:0;'>AttendEase</h1>"
             + "    <p style='color:#94a3b8;font-size:14px;margin-top:4px;'>Password Reset</p>"
             + "  </div>"
             + "  <p style='color:#f1f5f9;font-size:16px;'>Hi " + escapeHtml(name) + ",</p>"
             + "  <p style='color:#94a3b8;font-size:14px;line-height:1.6;'>"
             + "    We received a request to reset your password. Use the code below to set a new password. "
             + "    This code is valid for <strong style='color:#f1f5f9;'>15 minutes</strong>."
             + "  </p>"
             + "  <div style='background:#0f172a;border:2px solid #6366f1;border-radius:12px;padding:20px;text-align:center;margin:24px 0;'>"
             + "    <p style='color:#94a3b8;font-size:12px;margin:0 0 8px 0;text-transform:uppercase;letter-spacing:1px;'>Your Reset Code</p>"
             + "    <p style='color:#6366f1;font-size:22px;font-weight:bold;margin:0;letter-spacing:2px;word-break:break-all;'>" + escapeHtml(resetCode) + "</p>"
             + "  </div>"
             + "  <p style='color:#94a3b8;font-size:13px;line-height:1.5;'>"
             + "    If you didn't request this, you can safely ignore this email. Your password will remain unchanged."
             + "  </p>"
             + "  <hr style='border:none;border-top:1px solid #334155;margin:24px 0;' />"
             + "  <p style='color:#475569;font-size:12px;text-align:center;margin:0;'>"
             + "    &copy; AttendEase &mdash; Smart Attendance Management"
             + "  </p>"
             + "</div></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static String toJsonString(String text) {
        return "\"" + escapeJson(text) + "\"";
    }

    private static String envOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}