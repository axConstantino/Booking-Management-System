package com.axconstantino.reservationsystem.authtest;

import com.axconstantino.reservationsystem.BookingManagementSystemApplication;
import com.axconstantino.reservationsystem.auth.dto.AuthRequest;
import com.axconstantino.reservationsystem.auth.dto.RegisterRequest;
import com.axconstantino.reservationsystem.auth.dto.TokenResponse;
import com.axconstantino.reservationsystem.auth.service.AuthService;
import com.axconstantino.reservationsystem.common.exception.DuplicateEntityException;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(classes = BookingManagementSystemApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    void whenRegister_thenUserSavedAndTokensReturned() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "password123");
        TokenResponse resp = authService.register(req);
        assertThat(resp).isNotNull();
        assertThat(resp.accessToken()).isNotBlank();
        assertThat(resp.refreshToken()).isNotBlank();
        Optional<User> saved = userRepository.findByEmail("alice@example.com");
        assertThat(saved).isPresent();
        User user = saved.get();
        String stored = (String) redisTemplate.opsForValue().get("refresh:" + user.getId());
        assertThat(stored).isEqualTo(resp.refreshToken());
    }

    @Test
    void whenRegisterWithDuplicateEmail_thenThrows() {
        RegisterRequest req = new RegisterRequest("Carol", "carol@example.com", "pass");
        authService.register(req);
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateEntityException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void whenAuthenticate_thenTokensRotated() throws InterruptedException {
        RegisterRequest reg = new RegisterRequest("Bob", "bob@example.com", "secret");
        TokenResponse before = authService.register(reg);

        Thread.sleep(10);

        AuthRequest authReq = new AuthRequest("bob@example.com", "secret");
        TokenResponse after = authService.authenticate(authReq);
        assertThat(after.accessToken()).isNotBlank().isNotEqualTo(before.accessToken());
        assertThat(after.refreshToken()).isNotBlank().isNotEqualTo(before.refreshToken());
        User user = userRepository.findByEmail("bob@example.com").orElseThrow();
        String stored = (String) redisTemplate.opsForValue().get("refresh:" + user.getId());
        assertThat(stored).isEqualTo(after.refreshToken());
    }

    @Test
    void whenAuthenticateWithBadCredentials_thenThrows() {
        AuthRequest authReq = new AuthRequest("notfound@example.com", "wrong");
        assertThatThrownBy(() -> authService.authenticate(authReq))
                .isInstanceOf(Exception.class);
    }

    @Test
    void whenRefreshToken_thenNewTokensIssued() throws InterruptedException {
        RegisterRequest reg = new RegisterRequest("Dan", "dan@example.com", "mypwd");
        TokenResponse initial = authService.register(reg);

        Thread.sleep(10);

        String header = "Bearer " + initial.refreshToken();
        TokenResponse refreshed = authService.refreshToken(header);
        assertThat(refreshed.accessToken()).isNotBlank().isNotEqualTo(initial.accessToken());
        assertThat(refreshed.refreshToken()).isNotBlank().isNotEqualTo(initial.refreshToken());
    }

    @Test
    void whenRefreshWithInvalidHeader_thenThrows() {
        assertThatThrownBy(() -> authService.refreshToken("InvalidHeader"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid authentication scheme");
    }

    @Test
    void whenRefreshWithMismatchedToken_thenThrows() {
        RegisterRequest reg = new RegisterRequest("Eve", "eve@example.com", "pwd");
        TokenResponse resp = authService.register(reg);

        User user = userRepository.findByEmail("eve@example.com").orElseThrow();
        redisTemplate.delete("refresh:" + user.getId());
        String header = "Bearer " + resp.refreshToken();
        assertThatThrownBy(() -> authService.refreshToken(header))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Refresh token not found");
    }

    @Test
    void whenLogout_thenTokenRemoved() {
        RegisterRequest reg = new RegisterRequest("Frank", "frank@example.com", "xyz");
        TokenResponse resp = authService.register(reg);
        String header = "Bearer " + resp.refreshToken();
        authService.logout(header);
        User user = userRepository.findByEmail("frank@example.com").orElseThrow();
        assertThat(redisTemplate.opsForValue().get("refresh:" + user.getId())).isNull();
    }
}

