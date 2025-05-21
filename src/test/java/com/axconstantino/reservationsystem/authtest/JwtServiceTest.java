package com.axconstantino.reservationsystem.authtest;

import com.axconstantino.reservationsystem.auth.service.JwtService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.jsonwebtoken.JwtException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;

    private User mockUser;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        setPrivateField("secretKey", Base64.getEncoder().encodeToString("supersecretkey12345678901234567890".getBytes()));
        setPrivateField("accessExpiration", 3600000L);
        setPrivateField("refreshExpiration", 7200000L);

        mockUser = new User();
        mockUser.setEmail("test@example.com");
        mockUser.setName("Test User");
        mockUser.setUpdatedAt(LocalDateTime.now().minusDays(1));
        mockUser.setSecurityUpdatedAt(LocalDateTime.now().minusDays(1));
        mockUser.setRoles(Set.of(Role.USER));
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = JwtService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(jwtService, value);
    }

    @Test
    void testGenerateAndExtractUsername() {
        String token = jwtService.generateToken(mockUser);
        String extracted = jwtService.extractUserName(token);
        assertEquals(mockUser.getEmail(), extracted);
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken(mockUser);
        boolean isValid = jwtService.isTokenValid(token, mockUser);
        assertTrue(isValid);
    }

    @Test
    void testExpiredTokenReturnsFalse() throws Exception {
        setPrivateField("accessExpiration", -1000L); // token expirado
        String expiredToken = jwtService.generateToken(mockUser);
        boolean valid = jwtService.isTokenValid(expiredToken, mockUser);
        assertFalse(valid);
    }

    @Test
    void testExtractAllClaims() {
        String token = jwtService.generateToken(mockUser);
        Claims claims = jwtService.extractAllClaims(token);
        assertEquals(mockUser.getEmail(), claims.getSubject());
        assertNotNull(claims.get("name"));
        assertNotNull(claims.get("roles"));
    }

    @Test
    void testExtractClaim() {
        String token = jwtService.generateToken(mockUser);
        Optional<String> nameClaim = jwtService.extractClaim(token, c -> c.get("name", String.class));
        assertTrue(nameClaim.isPresent());
        assertEquals(mockUser.getName(), nameClaim.get());
    }

    @Test
    void testExtractAuthorities() {
        var authorities = jwtService.extractAuthorities(mockUser);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void testInvalidTokenThrowsException() {
        String invalidToken = "invalid.token.value";
        assertThrows(JwtException.class, () -> jwtService.extractUserName(invalidToken));
        assertThrows(JwtException.class, () -> jwtService.extractAllClaims(invalidToken));
        assertTrue(jwtService.extractClaim(invalidToken, Claims::getSubject).isEmpty());
        assertTrue(jwtService.isTokenExpired(invalidToken));
    }
}

