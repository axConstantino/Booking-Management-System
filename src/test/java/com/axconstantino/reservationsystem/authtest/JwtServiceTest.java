package com.axconstantino.reservationsystem.authtest;

import com.axconstantino.reservationsystem.auth.service.JwtService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private User user;
    private final String userEmail = "test@example.com";
    private final String userName = "Test User";
    private final Set<Role> userRoles = Set.of(Role.USER, Role.ADMIN);

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        SecretKey key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String base64Key = java.util.Base64.getEncoder().encodeToString(key.getEncoded());
        ReflectionTestUtils.setField(jwtService, "secretKey", base64Key);
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 60000L); // 1 minuto
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 120000L); // 2 minutos

        user = new User();
        user.setEmail(userEmail);
        user.setName(userName);
        user.setRoles(userRoles);
    }

    @Test
    void testExtractUserNameFromValidToken() {
        String token = jwtService.generateToken(user);
        String extractedEmail = jwtService.extractUserName(token);
        assertEquals(userEmail, extractedEmail);
    }

    @Test
    void testExtractUserNameFromInvalidTokenThrowsException() {
        String invalidToken = "invalid.token.here";
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtService.extractUserName(invalidToken));
    }

    @Test
    void testGenerateTokenIncludesCorrectClaims() {
        String token = jwtService.generateToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(jwtService.getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(userEmail, claims.getSubject());
        assertEquals(userName, claims.get("name", String.class));
        assertThat(claims.get("roles", List.class)).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void testRefreshTokenExpiresAfterAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(user);
        Date refreshExpiration = jwtService.extractExpiration(refreshToken);

        String accessToken = jwtService.generateToken(user);
        Date accessExpiration = jwtService.extractExpiration(accessToken);

        assertTrue(refreshExpiration.after(accessExpiration));
    }

    @Test
    void testValidTokenReturnsTrue() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void testTokenWithWrongUserIsInvalid() {
        User wrongUser = new User();
        wrongUser.setEmail("wrong@example.com");

        String token = jwtService.generateToken(user);
        assertFalse(jwtService.isTokenValid(token, wrongUser));
    }

    @Test
    void testExpiredTokenIsInvalid() {
        String expiredToken = Jwts.builder()
                .subject(userEmail)
                .issuedAt(new Date(System.currentTimeMillis() - 100000))
                .expiration(new Date(System.currentTimeMillis() - 50000))
                .signWith(jwtService.getSignInKey())
                .compact();

        assertFalse(jwtService.isTokenValid(expiredToken, user));
    }

    @Test
    void testExtractExpirationMatchesGenerated() {
        String token = jwtService.generateToken(user);
        Date expectedExpiration = new Date(System.currentTimeMillis() + 60000);
        Date actualExpiration = jwtService.extractExpiration(token);

        assertThat(actualExpiration.getTime())
                .isCloseTo(expectedExpiration.getTime(), within(2000L));
    }

    @Test
    void testExtractAuthoritiesFromToken() {
        String token = jwtService.generateToken(user);
        Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(token);

    }
}
