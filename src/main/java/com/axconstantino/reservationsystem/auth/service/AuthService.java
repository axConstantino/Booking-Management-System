package com.axconstantino.reservationsystem.auth.service;

import com.axconstantino.reservationsystem.auth.dto.AuthRequest;
import com.axconstantino.reservationsystem.auth.dto.RegisterRequest;
import com.axconstantino.reservationsystem.auth.dto.TokenResponse;
import com.axconstantino.reservationsystem.common.exception.ConflictException;
import com.axconstantino.reservationsystem.common.exception.DuplicateEntityException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.mail.EmailService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * AuthService handles user authentication and registration processes,
 * including issuing and refreshing JWT tokens and managing refresh tokens in Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Registers a new user and returns JWT and Refresh tokens.
     *
     * @param registerRequest The registration data.
     * @return TokenResponse with access and refresh tokens.
     */
    @Transactional
    public TokenResponse register(final RegisterRequest registerRequest) {
        log.info("Attempting to register user with email: {}", registerRequest.email());

        if (userRepository.existsByEmail(registerRequest.email())) {
            log.warn("Registration failed: Email already exists: {}", registerRequest.email());
            throw new DuplicateEntityException("Email already exists");
        }

        if (!isValidPassword(registerRequest.password())) {
            throw new ConflictException("Password does not meet complexity requirements");
        }

        final User user = User.builder()
                .name(registerRequest.name())
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .roles(Set.of(Role.USER))
                .build();

        final User savedUser = userRepository.save(user);

        final String jwtToken = jwtService.generateToken(savedUser);
        final String refreshToken = jwtService.generateRefreshToken(savedUser);
        saveRefreshToken(user.getId(), refreshToken);

        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());

        return new TokenResponse(jwtToken, refreshToken);
    }

    /**
     * Authenticates a user and returns new tokens.
     *
     * @param request AuthRequest containing email and password.
     * @return TokenResponse with new access and refresh tokens.
     */
    @Transactional
    public TokenResponse authenticate(final AuthRequest request) {
        log.info("Authenticating user: {}", request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User not found - {}", request.email());
                    return new NotFoundException(request.email());
                });

        final String accessToken = jwtService.generateToken(user);
        final String refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveRefreshToken(user.getId(), refreshToken);

        log.info("Authentication successful for user: {}", user.getEmail());
        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * Stores a refresh token in Redis with a 7-day TTL.
     *
     * @param id           User ID.
     * @param refreshToken Token to store.
     */
    private void saveRefreshToken(final UUID id, final String refreshToken) {
        log.debug("Saving refresh token for user ID: {}", id);
        redisTemplate.opsForValue().set("refresh:" + id, refreshToken, Duration.ofDays(7));
    }

    /**
     * Removes the stored refresh token for a user (revokes token).
     *
     * @param user The user whose token is being revoked.
     */
    public void revokeAllUserTokens(final User user) {
        log.debug("Revoking refresh token for user ID: {}", user.getId());
        redisTemplate.delete("refresh:" + user.getId());
    }

    /**
     * Validates and processes refresh token to issue new JWT tokens.
     *
     * @param authentication The Authorization header with "Bearer <token>".
     * @return New TokenResponse with access and refresh tokens.
     */
    @Transactional
    public TokenResponse refreshToken(@NotNull String authentication) {
        log.info("Attempting token refresh");

        try {
            validateAuthHeader(authentication);

            final String oldRefreshToken = authentication.substring(7);
            final String userEmail = jwtService.extractUserName(oldRefreshToken);

            final User user = getUserByEmail(userEmail);
            validateRefreshToken(oldRefreshToken, user);

            return generateNewTokens(user, oldRefreshToken);
        } catch (SecurityException ex) {
            log.error("Security exception during refresh token: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Validates that the authorization header is properly formatted.
     *
     * @param authHeader Header value.
     */
    private void validateAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid authentication scheme in header");
            throw new SecurityException("Invalid authentication scheme");
        }
    }

    /**
     * Retrieves user by email, throws if not found.
     *
     * @param email The user's email.
     * @return The found User entity.
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new NotFoundException("User not found");
                });
    }

    /**
     * Validates that the provided refresh token matches the one in Redis and is valid.
     *
     * @param token Refresh token provided by the client.
     * @param user  The user entity.
     */
    private void validateRefreshToken(String token, User user) {
        String storedToken = (String) redisTemplate.opsForValue().get("refresh:" + user.getId());

        if (storedToken == null) {
            log.warn("Refresh token not found in Redis for user: {}", user.getEmail());
            throw new SecurityException("Refresh token not found");
        }

        if (!storedToken.equals(token)) {
            log.warn("Refresh token mismatch for user: {}", user.getEmail());
            throw new SecurityException("Token mismatch");
        }

        if (!jwtService.isTokenValid(token, user)) {
            log.warn("Invalid token signature for user: {}", user.getEmail());
            throw new SecurityException("Invalid token signature");
        }
    }


    /**
     * Revokes all refresh tokens from the user (logout server-side).
     *
     * @param authHeader the header “ Authorization: Bearer <refreshToken> ”
     */
    @Transactional
    public void logout(String authHeader) {
        validateAuthHeader(authHeader);
        String refreshToken = authHeader.substring(7);
        String userEmail = jwtService.extractUserName(refreshToken);
        User user = getUserByEmail(userEmail);
        // We delete the token in Redis
        revokeAllUserTokens(user);
        log.info("User logged out and tokens revoked: {}", userEmail);
    }

    /**
     * Generates and returns new access and refresh tokens for the user.
     *
     * @param user     The user.
     * @param oldToken The old refresh token (used for logging or cleanup).
     * @return New TokenResponse.
     */
    private TokenResponse generateNewTokens(User user, String oldToken) {
        redisTemplate.delete("refresh:" + user.getId());
        log.info("Generating new tokens for user: {}", user.getEmail());

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user.getId(), newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    private boolean isValidPassword(String password) {
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        return password.matches(pattern);
    }

}
