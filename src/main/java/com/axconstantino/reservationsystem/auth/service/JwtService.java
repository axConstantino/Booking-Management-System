package com.axconstantino.reservationsystem.auth.service;

import com.axconstantino.reservationsystem.user.database.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.lang.Function;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtService {

    @Value("${app.security.jwt.secret}")
    private String secretKey;

    @Value("${app.security.jwt.access-expiration}")
    private long accessExpiration;

    @Value("${app.security.jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Extracts the username (email) from the JWT token.
     *
     * @param token the JWT token.
     * @return the email (subject) from the token.
     */
    public String extractUserName(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            log.debug("Extracted subject '{}' from JWT token.", subject);
            return subject;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to extract username from JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param user the authenticated user.
     * @return the signed JWT access token.
     */
    public String generateToken(final User user) {
        log.debug("Generating access token for user '{}'.", user.getEmail());
        String token = buildToken(user, accessExpiration);
        log.info("Access token generated successfully for user '{}'.", user.getEmail());
        return token;
    }

    /**
     * Generates a JWT refresh-token for the given user.
     *
     * @param user the authenticated user.
     * @return the signed JWT refresh-token.
     */
    public String generateRefreshToken(final User user) {
        log.debug("Generating refresh token for user '{}'.", user.getEmail());
        String token = buildToken(user, refreshExpiration);
        log.info("Refresh token generated successfully for user '{}'.", user.getEmail());
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
        log.debug("Building JWT token with expiration {} ms for user '{}'.", expirationMillis, user.getEmail());

        String updatedAt = user.getUpdatedAt()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String securityUpdatedAt = user.getSecurityUpdatedAt()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", Optional.ofNullable(user.getName()).orElse(""));
        claims.put("roles", roles);
        claims.put("updatedAt", updatedAt);
        claims.put("securityUpdatedAt", securityUpdatedAt);
        claims.put("once", UUID.randomUUID().toString());

        long now = System.currentTimeMillis();

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMillis))
                .signWith(getSignInKey())
                .compact();

        log.debug("JWT token built successfully for user '{}'.", user.getEmail());
        return token;
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
                log.debug("JWT token is valid for user '{}'.", username);
            } else {
                log.warn("Invalid JWT token for user '{}'. Either username mismatch or token expired.", username);
            }
            return isValid;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Exception during token validation for user '{}': {}", user.getEmail(), e.getMessage());
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
        try {
            Date expiration = extractExpiration(token);
            long allowedClockSkew = 60 * 1000; // 1 minute skew allowed
            long currentTime = System.currentTimeMillis();

            boolean expired = expiration.getTime() < (currentTime - allowedClockSkew);
            if (expired) {
                log.warn("JWT token expired at {} (current time {}, allowed skew {}).",
                        expiration, new Date(currentTime), allowedClockSkew);
            } else {
                log.debug("JWT token not expired. Expiration time: {}", expiration);
            }
            return expired;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            // Consider expired if cannot parse expiration
            return true;
        }
    }

    /**
     * Extracts the expiration date from the token.
     *
     * @param token the JWT token.
     * @return the expiration date.
     */
    public Date extractExpiration(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();

            log.debug("Extracted expiration date '{}' from JWT token.", expiration);
            return expiration;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to extract expiration from JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract authorities from the user roles.
     *
     * @param user the user.
     * @return collection of granted authorities.
     */
    public Collection<? extends GrantedAuthority> extractAuthorities(User user) {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }

    /**
     * Extract a specific claim from the token.
     *
     * @param token the JWT token.
     * @param claimsResolver function to extract claim.
     * @param <T> type of claim.
     * @return optional claim.
     */
    public <T> Optional<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            Claims claims = extractAllClaims(token);
            return Optional.ofNullable(claimsResolver.apply(claims));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Error extracting claim from JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract all claims from the token.
     *
     * @param token the JWT token.
     * @return claims.
     */
    public Claims extractAllClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Successfully parsed all claims from JWT token.");
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error parsing JWT claims: {}", e.getMessage());
            throw new JwtException("Invalid JWT token");
        }
    }

    /**
     * Decodes the secret key from base64 and returns a {@link SecretKey} instance for signing.
     *
     * @return the HMAC SHA secret key.
     */
    public SecretKey getSignInKey() {
        try {
            final byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            log.debug("Successfully decoded secret key for JWT signing.");
            return key;
        } catch (IllegalArgumentException e) {
            log.error("Invalid secret key format for JWT: {}", e.getMessage());
            throw e;
        }
    }
}
