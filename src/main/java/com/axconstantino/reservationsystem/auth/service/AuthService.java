package com.axconstantino.reservationsystem.auth.service;

import com.axconstantino.reservationsystem.auth.dto.AuthRequest;
import com.axconstantino.reservationsystem.auth.dto.RegisterRequest;
import com.axconstantino.reservationsystem.auth.dto.TokenResponse;
import com.axconstantino.reservationsystem.common.exception.DuplicateEntityException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.service.UserService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public TokenResponse register(final RegisterRequest registerRequest) {

        final User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles(Set.of(Role.ROLE_USER))
                .build();

        final User savedUser = userRepository.save(user);
        final String jwtToken = jwtService.generateToken(savedUser);
        final String refreshToken = jwtService.generateRefreshToken(savedUser);
        saveRefreshToken(user.getId(), refreshToken);
        return new TokenResponse(jwtToken, refreshToken);
    }

    @Transactional
    public TokenResponse authenticate(final AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException(request.email()));
        final String accessToken = jwtService.generateToken(user);
        final String refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveRefreshToken(user.getId(), refreshToken);
        return new TokenResponse(accessToken, refreshToken);
    }

    private void saveRefreshToken(final UUID id, final String refreshToken) {
        redisTemplate.opsForValue().set("refresh:" + id, refreshToken, Duration.ofDays(7));
    }

    private void revokeAllUserTokens(final User user) {
        redisTemplate.delete("refresh:" + user.getId());
    }

    @Transactional
    public TokenResponse refreshToken(@NotNull String authentication) {
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

    private void validateAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Invalid authentication scheme");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateRefreshToken(String token, User user) {
        String storedToken = (String) redisTemplate.opsForValue().get("refresh:" + user.getId());

        if (storedToken == null) {
            throw new SecurityException("Refresh token not found");
        }

        if (!storedToken.equals(token)) {
            throw new SecurityException("Token mismatch");
        }

        if (!jwtService.isTokenValid(token, user)) {
            throw new SecurityException("Invalid token signature");
        }
    }

    private TokenResponse generateNewTokens(User user, String oldToken) {
        redisTemplate.delete("refresh:" + user.getId());

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user.getId(), newRefreshToken);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

}
