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
        new Thread(() -> {
            try {
                Properties prop = new Properties();
                prop.put("mail.smtp.host", "smtp-relay.brevo.com");
                prop.put("mail.smtp.port", "465");
                prop.put("mail.smtp.auth", "true");
                
                // CRITICAL: These three lines enable SSL for Port 465
                prop.put("mail.smtp.ssl.enable", "true");
                prop.put("mail.smtp.socketFactory.port", "465");
                prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                
                // Increased timeouts to give the network more time
                prop.put("mail.smtp.connectiontimeout", "10000"); 
                prop.put("mail.smtp.timeout", "10000");

                Session session = Session.getInstance(prop, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail));
                message.setSubject("AttendEase - Password Reset Code");
                
                String content = "Hello,\n\nYour password reset code is: " + token + 
                                 "\n\nPlease enter this code in the app to reset your password.";
                
                message.setText(content);

                System.out.println("[EmailService] Attempting to send via Port 465...");
                Transport.send(message);
                System.out.println("[EmailService] SUCCESS! Sent to: " + targetEmail);

            } catch (Exception e) {
                System.err.println("[EmailService] Still failing on Port 465. This usually means the cloud provider is blocking all SMTP traffic.");
                e.printStackTrace();
            }
        }).start();
    }
}