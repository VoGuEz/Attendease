package com.attendease.services;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailService {

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUser;
    private final String smtpPassword;
    private final String fromAddress;
    private final boolean enabled;

    public EmailService() {
        this.smtpHost = envOrDefault("SMTP_HOST", "smtp.gmail.com");
        this.smtpPort = Integer.parseInt(envOrDefault("SMTP_PORT", "587"));
        this.smtpUser = System.getenv("SMTP_USER");
        this.smtpPassword = System.getenv("SMTP_PASSWORD");
        this.fromAddress = envOrDefault("SMTP_FROM", this.smtpUser);
        this.enabled = smtpUser != null && !smtpUser.isBlank()
                    && smtpPassword != null && !smtpPassword.isBlank();

        if (!enabled) {
            System.out.println("[EmailService] SMTP not configured (SMTP_USER / SMTP_PASSWORD missing). Email sending disabled.");
        } else {
            System.out.println("[EmailService] SMTP configured: " + smtpHost + ":" + smtpPort + " from " + fromAddress);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void sendResetCode(String toEmail, String resetCode, String userName) {
        if (!enabled) {
            System.out.println("[EmailService] Skipping email (SMTP not configured). Reset code for " + toEmail + ": " + resetCode);
            return;
        }

        String subject = "AttendEase - Password Reset Code";
        String htmlBody = buildResetEmailHtml(resetCode, userName);

        try {
            sendEmail(toEmail, subject, htmlBody);
            System.out.println("[EmailService] Reset code sent to " + toEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Failed to send reset email. Please try again later.");
        }
    }

    private void sendEmail(String to, String subject, String htmlBody) throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", smtpHost);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (smtpPort == 465) {
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        System.out.println("[EmailService] Connecting to " + smtpHost + ":" + smtpPort + " to send email to " + to);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress, "AttendEase"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=utf-8");

        Transport.send(message);
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

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static String envOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
