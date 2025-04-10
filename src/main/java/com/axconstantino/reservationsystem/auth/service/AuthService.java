package com.axconstantino.reservationsystem.auth.service;

import com.axconstantino.reservationsystem.auth.dto.AuthRequest;
import com.axconstantino.reservationsystem.auth.dto.RegisterRequest;
import com.axconstantino.reservationsystem.auth.dto.TokenResponse;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
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
                .build();

        final User savedUser = userRepository.save(user);
        final String jwtToken = jwtService.generateToken(savedUser);
        final String refreshToken = jwtService.generateRefreshToken(savedUser);

        redisTemplate.opsForValue().set("refresh:" + savedUser.getId(), refreshToken, Duration.ofDays(7));
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
        redisTemplate.opsForValue().set("refresh:" + user.getId(), refreshToken, Duration.ofDays(7));
        return new TokenResponse(accessToken, refreshToken);
    }

    private void revokeAllUserTokens(final User user) {
        redisTemplate.delete("refresh:" + user.getId());
    }

    public TokenResponse refreshToken(@NotNull String authentication) {
        if (authentication == null || !authentication.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid auth header");
        }

        final String refreshToken = authentication.substring(7);
        final String userEmail = jwtService.extractUserName(refreshToken);

        if (userEmail == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        final User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        final boolean isTokenValid = jwtService.isTokenValid(refreshToken, user);

        if (!isTokenValid) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        revokeAllUserTokens(user);
        final String storedRefreshToken = (String) redisTemplate.opsForValue().get("refresh:" + user.getId());
        if (!refreshToken.equals(storedRefreshToken)) {
            throw new IllegalArgumentException("Refresh token not recognized");
        }

        final String accessToken = jwtService.generateToken(user);

        return new TokenResponse(accessToken, refreshToken);
    }

}
