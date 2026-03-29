package com.attendease.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {
    private final String username;
    private final String password;
    private final String fromEmail;

    public EmailService() {
        // These pull directly from your Railway Variables
        this.username = System.getenv("SMTP_FROM"); // Your Brevo login email
        this.password = System.getenv("BREVO_API_KEY"); // Your Brevo API Key or App Password
        this.fromEmail = System.getenv("SMTP_FROM");
    }

    public void sendResetCode(String targetEmail, String token) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp-relay.brevo.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail));
            message.setSubject("AttendEase - Your Password Reset Code");
            
            String content = "Hello,\n\nYour password reset code is: " + token + 
                             "\n\nPlease enter this code in the app to reset your password.";
            
            message.setText(content);
            Transport.send(message);
            System.out.println("[EmailService] Reset code sent to: " + targetEmail);
        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send email!");
            e.printStackTrace();
        }
    }
}