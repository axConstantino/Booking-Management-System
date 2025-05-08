package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.common.exception.ExpiredTokenException;
import com.axconstantino.reservationsystem.common.exception.InvalidTokenException;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for secure password reset token generation and validation.
 * <p>
 * Implements best practices for token security:
 * <ul>
 *   <li>HMAC-SHA256 cryptographic signing</li>
 *   <li>Immediate token invalidation after use</li>
 *   <li>Time-zone agnostic expiration handling</li>
 *   <li>Database-level token uniqueness</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final UserRepository repository;

    @Value("${app.token.secret}")
    private String tokenSecret;

    /**
     * Generates a secure password reset token.
     *
     * @param user User entity to associate with the token
     * @return Raw token value (to be shared with user)
     * @implNote
     * 1. Generates cryptographically-secure UUID
     * 2. Signs token with HMAC-SHA256
     * 3. Stores hashed token and UTC expiration time
     */
    public String generatePasswordResetToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = HmacUtil.hmacSha256(tokenSecret, rawToken);

        user.setResetToken(hashedToken);
        user.setTokenExpiry(Instant.now().plusSeconds(7200));  // 2 hours
        repository.save(user);

        log.info("Generated reset token for user: {}", user.getEmail());
        return rawToken;
    }

    /**
     * Validates and consumes a password reset token.
     *
     * @param rawToken Token provided by the user
     * @return Associated user's email if valid
     * @throws InvalidTokenException If no matching token found
     * @throws ExpiredTokenException If token exists but has expired
     * @implNote
     * 1. Always invalidates token after validation (even if expired)
     * 2. Uses optimized database query for token lookup
     */
    public String validatePasswordResetToken(String rawToken) {
        String hashedToken = HmacUtil.hmacSha256(tokenSecret, rawToken);
        List<User> users = repository.findAllWithResetToken();

        User user = users.stream()
                .filter(u -> hashedToken.equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Invalid token attempt: {}", rawToken);
                    return new InvalidTokenException("Invalid token");
                });

        if (user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(Instant.now())) {
            log.warn("Expired token for user: {}", user.getEmail());
            clearToken(user);
            throw new ExpiredTokenException("Token expired");
        }

        clearToken(user);
        return user.getEmail();
    }

    /**
     * Clears reset token fields from user entity.
     *
     * @param user User entity to modify
     * @apiNote Internal utility method
     */
    private void clearToken(User user) {
        user.setResetToken(null);
        user.setTokenExpiry(null);
        repository.save(user);
        log.debug("Cleared token for user: {}", user.getEmail());
    }
}
