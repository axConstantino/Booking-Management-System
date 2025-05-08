package com.axconstantino.reservationsystem.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending emails to users.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.password.reset.url}")
    private String resetPasswordUrl;

    public void sendWelcomeEmail(String email, String name) {
        String subject = "Welcome to Our Hotel Booking Platform!";
        String body = "Hello " + name + ",\n\n"
                + "Welcome! We're excited to have you on board.\n"
                + "Start exploring and book the best hotel rooms today.\n\n"
                + "If you have any questions, feel free to reach out to our support team.\n\n"
                + "Best regards,\n"
                + "The Hotel Booking Team";

        sendEmail(email, subject, body);
    }

    public void sendPasswordResetEmail(String email, String token) {
        String subject = "Reset your password";
        String body = "To reset your password, please click the link below:\n"
                + resetPasswordUrl + "?token=" + token + "\n\n"
                + "This link will expire in 2 hours.\n"
                + "If you didn't request this change, you can safely ignore this email.";

        sendEmail(email, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("olveraconstantinoaxel@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
