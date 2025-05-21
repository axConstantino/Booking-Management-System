package com.axconstantino.reservationsystem.auth.security;

import com.axconstantino.reservationsystem.auth.service.JwtService;
import com.axconstantino.reservationsystem.common.exception.JwtAuthenticationException;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * JwtAuthenticationFilter intercepts HTTP requests to authenticate users based on JWT tokens.
 * <p>
 * This filter extracts the JWT from the Authorization header, validates its integrity,
 * checks the user's existence and token consistency against the last update timestamps,
 * and sets the SecurityContext for authenticated requests.
 * <p>
 * Tokens are invalidated when user data changes, based on the claims:
 * - updatedAt: Reflects general user profile updates.
 * - securityUpdatedAt: Reflects changes in sensitive security info (e.g., password).
 * <p>
 * If token validation fails, the request is rejected with HTTP 401 Unauthorized.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String UPDATED_AT_CLAIM = "updatedAt";

    private static final String SECURITY_UPDATED_AT_CLAIM = "securityUpdatedAt";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    /**
     * Filters each HTTP request to perform JWT authentication.
     * <p>
     * Extracts and validates JWT token, checks user existence, verifies token consistency,
     * and sets authentication in the SecurityContext.
     * <p>
     * On failure, clears the context and returns HTTP error response.
     *
     * @param request     HTTP servlet request
     * @param response    HTTP servlet response
     * @param filterChain Filter chain to pass request and response forward
     * @throws ServletException in case of servlet errors
     * @throws IOException      in case of I/O errors
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Optional<String> jwt = extractToken(request);

            if (jwt.isPresent()) {
                String userEmail = extractAndValidateEmail(jwt.get());
                User user = loadUser(userEmail);
                validateToken(jwt.get(), user);
                setSecurityContext(user);
            }
        } catch (JwtAuthenticationException ex) {
            log.warn("Authentication failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            return;
        } catch (Exception ex) {
            log.error("Unexpected error during authentication", ex);
            SecurityContextHolder.clearContext();
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
            return;
        }

        filterChain.doFilter(request, response);
    }


    /**
     * Extracts the JWT token from the Authorization header.
     *
     * @param request HTTP servlet request
     * @return Optional with JWT token if present and valid format; empty otherwise
     */
    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        return Optional.of(header.substring(BEARER_PREFIX.length()));
    }

    /**
     * Extracts the user email (username) from the JWT token and validates token integrity.
     *
     * @param token JWT token
     * @return user email contained in token
     * @throws JwtAuthenticationException if token is invalid or malformed
     */
    private String extractAndValidateEmail(String token) {
        try {
            return jwtService.extractUserName(token);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            throw new JwtAuthenticationException("Invalid credentials");
        }
    }

    /**
     * Loads the User entity from the repository by email.
     *
     * @param email user email
     * @return User entity
     * @throws JwtAuthenticationException if user is not found
     */
    private User loadUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new JwtAuthenticationException("Invalid credentials");
                });

        if (user.getUpdatedAt() == null || user.getSecurityUpdatedAt() == null) {
            log.warn("User timestamps missing for: {}", email);
            throw new JwtAuthenticationException("Invalid user state");
        }

        return user;
    }

    /**
     * Validates the token's validity and consistency with the current user state.
     *
     * @param token JWT token
     * @param user  User entity
     * @throws JwtAuthenticationException if token is invalid or inconsistent
     */
    private void validateToken(String token, User user) {
        if (!jwtService.isTokenValid(token, user)) {
            log.warn("Token validation failed for user: {}", user.getEmail());
            throw new JwtAuthenticationException("Invalid credentials");
        }

        validateTokenConsistency(token, user);
    }

    /**
     * Validates that the 'updatedAt' and 'securityUpdatedAt' claims in the token
     * match the user's current timestamps to prevent reuse of outdated tokens.
     *
     * @param token JWT token
     * @param user  User entity
     * @throws JwtAuthenticationException if timestamps do not match (token revoked)
     */
    private void validateTokenConsistency(String token, User user) {
        Optional<String> tokenUpdatedAt = jwtService.extractClaim(token,
                claims -> claims.get(UPDATED_AT_CLAIM, String.class));

        Optional<String> tokenSecurityUpdatedAt = jwtService.extractClaim(token,
                claims -> claims.get(SECURITY_UPDATED_AT_CLAIM, String.class));

        if (tokenUpdatedAt.isEmpty() || tokenSecurityUpdatedAt.isEmpty()) {
            log.warn("Invalid token format for user: {}", user.getEmail());
            throw new JwtAuthenticationException("Invalid token");
        }

        String userUpdatedAt = user.getUpdatedAt()
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMATTER);

        String userSecurityUpdatedAt = user.getSecurityUpdatedAt()
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMATTER);

        if (!userUpdatedAt.equals(tokenUpdatedAt.get())) {
            log.warn("Token revoked for {}: profile updated", user.getEmail());
            throw new JwtAuthenticationException("Session expired");
        }

        if (!userSecurityUpdatedAt.equals(tokenSecurityUpdatedAt.get())) {
            log.warn("Token revoked for {}: security updated", user.getEmail());
            throw new JwtAuthenticationException("Session expired");
        }
    }

    /**
     * Builds UserDetails and sets the authentication token in Spring Security context.
     *
     * @param user authenticated user entity
     */
    private void setSecurityContext(User user) {
        UserDetails userDetails = buildUserDetails(user);
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null, // credentials not stored here for security
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    /**
     * Converts application User entity to Spring Security UserDetails.
     *
     * @param user application user
     * @return UserDetails instance
     */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .accountLocked(user.isAccountLocked())
                .credentialsExpired(user.isCredentialsExpired())
                .authorities(jwtService.extractAuthorities(user))
                .build();
    }

}
