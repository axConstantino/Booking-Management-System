package com.axconstantino.reservationsystem.auth.service;

import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for handling JWT-related operations such as:
 * <ul>
 *     <li>Generating access and refresh tokens</li>
 *     <li>Extracting username (email) from token</li>
 *     <li>Validating token integrity and expiration</li>
 * </ul>
 * <p>
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access.expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh.expiration}")
    private long refreshExpiration;

    /**
     * Extracts the username (email) from the JWT token.
     *
     * @param token the JWT token.
     * @return the email (subject) from the token.
     */
    public String extractUserName(String token) {
        String subject = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        log.debug("Extracted subject '{}' from JWT.", subject);
        return subject;
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param user the authenticated user.
     * @return the signed JWT access token.
     */
    public String generateToken(final User user) {
        String token = buildToken(user, accessExpiration);
        log.info("Generated access token for user '{}'.", user.getEmail());
        return token;

    }

    /**
     * Generates a JWT refresh-token for the given user.
     *
     * @param user the authenticated user.
     * @return the signed JWT refresh-token.
     */
    public String generateRefreshToken(final User user) {
        String token = buildToken(user, refreshExpiration);
        log.info("Generated refresh token for user '{}'.", user.getEmail());
        return token;
    }

    /**
     * Builds a JWT token with custom claims for the specified user and expiration time.
     *
     * @param user the user to include in claims.
     * @param expirationMillis the expiration in milliseconds.
     * @return the signed JWT token.
     */
    private String buildToken(final User user, final long expirationMillis) {
        Set<Role> rolesSet = Optional.ofNullable(user.getRoles())
                .orElse(Collections.emptySet());
        List<String> roles = rolesSet.stream()
                .map(Enum::name)
                .toList();

        // Handle null name by defaulting to empty string
        String userName = Optional.ofNullable(user.getName()).orElse("");

        Map<String, Object> claims = Map.of(
                "name", userName,  // Use the null-checked name
                "roles", roles    // Use the processed roles list
        );

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validate if the token matches the user and is not expired.
     *
     * @param token the JWT token.
     * @param user the user to validate to against.
     * @return true if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, User user) {
        try {
            final String username = extractUserName(token);
            boolean isValid = username.equals(user.getEmail()) && !isTokenExpired(token);
            if (isValid) {
                log.debug("Token is valid for user '{}'.", username);
            } else {
                log.warn("Invalid token for user '{}'.", username);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Failed to validate token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether the token is expired based on its 'exp' claim.
     *
     * @param token the JWT token.
     * @return true if expired, false otherwise.
     */
    public boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        long allowedClockSkew = 60 * 1000;
        long currentTime = System.currentTimeMillis();

        boolean expired = expiration.getTime() < (currentTime - allowedClockSkew);
        if (expired) {
            log.warn("Token expired at {}.", expiration);
        }
        return expired;
    }

    /**
     * Extracts the expiration date from the token.
     *
     * @param token the JWT token.
     * @return the expiration date.
     */
    public Date extractExpiration(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        log.debug("Token expiration: {}", expiration);
        return expiration;
    }

    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<?> roles = claims.get("roles", List.class);

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString()))
                .collect(Collectors.toList());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Decodes the secret key from base64 and returns a {@link SecretKey} instance for signing.
     *
     * @return the HMAC SHA secret key.
     */
    public SecretKey getSignInKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
