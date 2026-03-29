package com.attendease.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {
    private final String username;
    private final String password;
    private final String fromEmail;

    public EmailService() {
        this.username = System.getenv("SMTP_FROM"); 
        this.password = System.getenv("BREVO_API_KEY"); 
        this.fromEmail = System.getenv("SMTP_FROM");
    }

    public void sendResetCode(String targetEmail, String token) {
        // Run in background thread so the frontend doesn't hang
        new Thread(() -> {
            try {
                Properties prop = new Properties();
                prop.put("mail.smtp.host", "smtp-relay.brevo.com");
                prop.put("mail.smtp.port", "587");
                prop.put("mail.smtp.auth", "true");
                prop.put("mail.smtp.starttls.enable", "true");
                prop.put("mail.smtp.connectiontimeout", "5000"); // 5 second limit
                prop.put("mail.smtp.timeout", "5000");

                Session session = Session.getInstance(prop, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail));
                message.setSubject("AttendEase - Password Reset");
                message.setText("Your reset code is: " + token);

                Transport.send(message);
                System.out.println("[EmailService] Successfully sent to: " + targetEmail);
            } catch (Exception e) {
                System.err.println("[EmailService] FAILED to send email. Check Brevo SMTP credentials.");
                e.printStackTrace();
            }
        }).start();
    }
}