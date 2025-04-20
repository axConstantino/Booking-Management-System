package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.common.exception.ConflictException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.dto.ChangePasswordRequest;
import com.axconstantino.reservationsystem.user.dto.ResetPasswordRequest;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.validation.PhoneValidator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class UserService extends BaseCRUDService<User, UserDTO, UUID, UserRepository, UserMapper> {
    private final PhoneValidator phoneValidator;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JavaMailSender mailSender;

    public UserService(UserRepository repository, UserMapper mapper, PhoneValidator phoneValidator, PasswordEncoder passwordEncoder, TokenService tokenService, JavaMailSender mailSender) {
        super(repository, mapper);
        this.phoneValidator = phoneValidator;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailSender = mailSender;
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));
    }

    @Transactional
    public User updateUserBasicInfo(String email, UserDTO updateRequest) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));

        mapper.updateFromDTO(user, updateRequest);

        if (updateRequest.getPhone() != null) {
            user.setPhone(phoneValidator.formatToE164(updateRequest.getPhone()));
        }

        return repository.save(user);
    }

    @Transactional
    public User addPhone(String email, String phone) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));

        if (user.getPhone() != null) {
            throw new RuntimeException("You already have a phone number");
        }

        user.setPhone(phoneValidator.formatToE164(phone));
        return repository.save(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new SecurityException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = tokenService.validateEmailVerificationToken(token);

        if (user.getEmailVerified()) {
            throw new ConflictException("The email has already been verified previously");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        repository.save(user);
        log.info("Email successfully verified for user: {}", user.getEmail());
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        String resetToken = tokenService.generatePasswordResetToken(user);
        sendResetEmail(user.getEmail(), resetToken);
    }

    private void sendResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Request");
        message.setText("Your password reset token: " + token);
        mailSender.send(message);
    }

    @Transactional
    public void completePasswordReset(ResetPasswordRequest request) {
        String email = tokenService.validatePasswordResetToken(request.getToken());
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    public void sendVerificationEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Verify your email");
            helper.setText(buildEmailContent(user), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Error sending verification email to {}", user.getEmail(), e);
            throw new RuntimeException("Error al enviar email de verificación");
        }
    }

    private String buildEmailContent(User user) {
        return  "<html>"
                + "<body style='font-family: Arial, sans-serif;'>"
                + "<h2 style='color: #2c3e50;'>Email Verification</h2>"
                + "<p>Please click the button below to verify your email:</p>"
                + "<a href='https://axConstantino.com/verify?token=" + user.getEmailVerificationToken() + "'"
                + " style='display: inline-block; background-color: #3498db; color: white; padding: 10px 20px;"
                + " text-decoration: none; border-radius: 5px;'>Verify Email</a>"
                + "</body></html>";

    }
}