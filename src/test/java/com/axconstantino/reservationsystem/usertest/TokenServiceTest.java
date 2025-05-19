package com.axconstantino.reservationsystem.usertest;


import com.axconstantino.reservationsystem.common.exception.ExpiredTokenException;
import com.axconstantino.reservationsystem.common.exception.InvalidTokenException;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.service.HmacUtil;
import com.axconstantino.reservationsystem.user.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TokenServiceTest {

    @Mock private UserRepository repository;
    @InjectMocks private TokenService tokenService;

    @Value("${app.security.token.password-reset}")
    private String tokenSecret = "secretKey";

    private User user;
    private String rawToken;
    private String hashedToken;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(tokenService, "tokenSecret", tokenSecret);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();
        rawToken = "raw-token-uuid";
        hashedToken = HmacUtil.hmacSha256(tokenSecret, rawToken);
    }

    @Test
    void generatePasswordResetToken_ShouldSaveHashedTokenAndExpiry() {
        given(repository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        String result = tokenService.generatePasswordResetToken(user);

        assertNotNull(result);
        assertNotNull(user.getResetToken());
        assertNotNull(user.getTokenExpiry());
        then(repository).should().save(user);
    }

    @Test
    void validatePasswordResetToken_ShouldThrowInvalid_WhenNoMatch() {
        given(repository.findAllWithResetToken()).willReturn(List.of(user));

        assertThrows(InvalidTokenException.class,
                () -> tokenService.validatePasswordResetToken(rawToken)
        );
    }

    @Test
    void validatePasswordResetToken_ShouldThrowExpired_WhenTokenExpired() {
        user.setResetToken(hashedToken);
        user.setTokenExpiry(Instant.now().minusSeconds(10));
        given(repository.findAllWithResetToken()).willReturn(List.of(user));

        assertThrows(ExpiredTokenException.class,
                () -> tokenService.validatePasswordResetToken(rawToken)
        );
        then(repository).should().save(user);
    }

    @Test
    void validatePasswordResetToken_ShouldReturnEmailAndClearToken_WhenValid() {
        user.setResetToken(hashedToken);
        user.setTokenExpiry(Instant.now().plusSeconds(3600));
        given(repository.findAllWithResetToken()).willReturn(List.of(user));
        given(repository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        String email = tokenService.validatePasswordResetToken(rawToken);

        assertEquals(user.getEmail(), email);
        assertNull(user.getResetToken());
        assertNull(user.getTokenExpiry());
        then(repository).should().save(user);
    }
}

