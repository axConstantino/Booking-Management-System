package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for generating and validating
 * password reset tokens for User entities.
 *<p>
 *     Tokens are randoms UUID, encoded before storage to prevent
 *     plaintexts leaks, and have limited validity period.
 *</p>
 */
@Service
@RequiredArgsConstructor
public class TokenService {
    /**
     * Repository for User entities, used to persist tokens state.
     */
    private final UserRepository repository;

    /**
     * Encoder for hashing token values prior to storage.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Generate a one-time password reset token for the given user.
     * <p>
     *     Steps performed:
     *     <ol>
     *         <li>Generate a random UUID as the raw token value.</li>
     *         <li>Encode the token used the configured {@link PasswordEncoder}.</li>
     *         <li>Store the encoded token and expiration timestamp (2 hours from now)
     *             on the {@code User} entity.</li>
     *         <li>Save the updated user record to the database.</li>
     *         <li>Return the raw (unencoded) token so it can be emailed to the user.</li>
     *     </ol>
     * </p>
     * @param user the {@link User} for whom the token is generated.
     * @return the raw token string (UUID) to be delivered to the user.
     */
    public String generatePasswordResetToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        String encodedToken = passwordEncoder.encode(tokenValue);

        user.setResetToken(encodedToken);
        user.setTokenExpiry(LocalDateTime.now().plusHours(2));
        repository.save(user);

        return tokenValue;
    }

    /**
     * Validates a password reset token and returns the associated user's email.
     * <p>
     *     Validation logic:
     *     <ul>
     *         <li>Encode the provided raw token and attempt to look up a matching user</li>
     *         <li>If not matching user is found, throw a {@link SecurityException}.</li>
     *         <li>If the token has expired (current time is after stored expiry),
     *             throw a {@link SecurityException}.</li>
     *         <li>Otherwise, return the user's email for downstream password-reset logic.</li>
     *     </ul>
     * </p>
     * @param token the raw token string provided by the client.
     * @return the email address of the user who owns this token.
     * @throws SecurityException if the token is invalid or has expired
     */
    public String validatePasswordResetToken(String token) {
        // Note: to match the stored encoded token we must encode the provided token.
        User user = repository.findByResetToken(passwordEncoder.encode(token))
                .orElseThrow(() -> new SecurityException("Invalid token"));

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new SecurityException("Token expired");
        }

        return user.getEmail();
    }

}
