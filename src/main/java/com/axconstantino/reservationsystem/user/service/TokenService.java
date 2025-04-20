package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;


    public String generatePasswordResetToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        String encodedToken = passwordEncoder.encode(tokenValue);

        user.setResetToken(encodedToken);
        user.setTokenExpiry(LocalDateTime.now().plusHours(2));
        repository.save(user);

        return tokenValue;
    }

    public String validatePasswordResetToken(String token) {
        User user = repository.findByResetToken(passwordEncoder.encode(token))
                .orElseThrow(() -> new SecurityException("Invalid token"));

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new SecurityException("Token expired");
        }

        return user.getEmail();
    }

    public String generateEmailVerificationToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        String encodedToken = passwordEncoder.encode(tokenValue);

        user.setEmailVerificationToken(encodedToken);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusDays(1));
        repository.save(user);

        return tokenValue;
    }

    public User validateEmailVerificationToken(String token) {
        User user = repository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new SecurityException("Invalid verification token"));

        if (user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new SecurityException("Verification token expired");
        }

        return user;
    }
}
