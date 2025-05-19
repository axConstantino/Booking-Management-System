package com.axconstantino.reservationsystem.mailtest;

import com.axconstantino.reservationsystem.mail.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendWelcomeEmail_ShouldSendEmailWithCorrectDetails() {
        String email = "user@example.com";
        String name = "John";

        emailService.sendWelcomeEmail(email, name);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getTo()).isEqualTo(new String[]{email});
        assertThat(sentMessage.getSubject()).isEqualTo("Welcome to Our Hotel Booking Platform!");
        assertThat(sentMessage.getText())
                .contains("Hello " + name)
                .contains("book the best hotel rooms today");
        assertThat(sentMessage.getFrom()).isEqualTo("olveraconstantinoaxel@gmail.com");
    }

    @Test
    void sendPasswordResetEmail_ShouldIncludeTokenInResetLink() {
        String email = "reset@example.com";
        String token = "SECURE_TOKEN_123";

        emailService.sendPasswordResetEmail(email, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertThat(sentMessage.getTo()).isEqualTo(new String[]{email});
        assertThat(sentMessage.getSubject()).isEqualTo("Reset your password");
        assertThat(sentMessage.getText())
                .contains("http://localhost:8080/api/v1/auth/reset-password?token=" + token)
                .contains("expire in 2 hours");
        assertThat(sentMessage.getFrom()).isEqualTo("olveraconstantinoaxel@gmail.com");
    }
}