package com.axconstantino.reservationsystem.usertest;

import com.axconstantino.reservationsystem.auth.service.AuthService;
import com.axconstantino.reservationsystem.common.exception.DuplicateEntityException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.mail.EmailService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.dto.ChangePasswordRequest;
import com.axconstantino.reservationsystem.user.dto.ResetPasswordRequest;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.user.service.TokenService;
import com.axconstantino.reservationsystem.user.service.UserService;
import com.axconstantino.reservationsystem.validation.PhoneValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository repository;
    @Mock private UserMapper mapper;
    @Mock private PhoneValidator phoneValidator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private EmailService emailService;
    @Mock private AuthService authService;

    @InjectMocks private UserService userService;

    private User user;
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("encodedPwd")
                .roles(roles)
                .build();
    }

    @Test
    void getUserByEmail_ShouldReturnUser_WhenExists() {
        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        User result = userService.getUserByEmail(email);
        assertEquals(user, result);
    }

    @Test
    void getUserByEmail_ShouldThrowNotFound_WhenMissing() {
        given(repository.findByEmail(email)).willReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUserByEmail(email));
    }

    @Test
    void updateUserBasicInfo_ShouldFormatMexicanPhoneAndUpdate() {
        UserDTO dto = new UserDTO();
        dto.setName("New Name");
        dto.setPhone("55 1234 5678");

        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        given(phoneValidator.formatToE164(dto.getPhone())).willReturn("+525512345678");
        willDoNothing().given(mapper).updateFromDTO(user, dto);
        given(repository.save(user)).willReturn(user);

        User updated = userService.updateUserBasicInfo(email, dto);

        then(mapper).should().updateFromDTO(user, dto);
        assertEquals("+525512345678", updated.getPhone());
    }

    @Test
    void updateUserBasicInfo_ShouldThrowNotFound_WhenMissing() {
        given(repository.findByEmail(email)).willReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.updateUserBasicInfo(email, new UserDTO()));
    }

    @Test
    void addUserRole_ShouldAddRoleAndRevokeTokens() {
        UUID id = user.getId();
        given(repository.findById(id)).willReturn(Optional.of(user));

        userService.addUserRole(id, Role.ADMIN);

        assertTrue(user.getRoles().contains(Role.ADMIN));
        then(authService).should().revokeAllUserTokens(user);
        then(repository).should().save(user);
    }

    @Test
    void addUserRole_ShouldThrowDuplicateEntity_WhenRoleExists() {
        UUID id = user.getId();
        given(repository.findById(id)).willReturn(Optional.of(user));
        assertThrows(DuplicateEntityException.class, () -> userService.addUserRole(id, Role.USER));
    }

    @Test
    void addUserRole_ShouldThrowNotFound_WhenMissing() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.addUserRole(id, Role.ADMIN));
    }

    @Test
    void addPhone_ShouldFormatMexicanPhoneAndSave() {
        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        given(phoneValidator.formatToE164("55-9876-5432")).willReturn("+525598765432");
        given(repository.save(user)).willReturn(user);

        User result = userService.addPhone(email, "55-9876-5432");

        assertEquals("+525598765432", result.getPhone());
        then(repository).should().save(user);
    }

    @Test
    void addPhone_ShouldThrowWhenAlreadyExists() {
        user.setPhone("+525598765432");
        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        assertThrows(IllegalStateException.class, () -> userService.addPhone(email, "55-9876-5432"));
    }

    @Test
    void addPhone_ShouldThrowNotFound_WhenMissing() {
        given(repository.findByEmail(email)).willReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.addPhone(email, "55-9876-5432"));
    }

    @Test
    void changePassword_ShouldUpdateWhenCorrectCurrent() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("rawPwd");
        req.setNewPassword("newRawPwd");
        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("rawPwd", user.getPassword())).willReturn(true);
        given(passwordEncoder.encode("newRawPwd")).willReturn("newEncodedPwd");

        userService.changePassword(email, req);

        assertEquals("newEncodedPwd", user.getPassword());
        then(repository).should().save(user);
    }

    @Test
    void changePassword_ShouldThrowWhenIncorrectCurrent() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrongPwd");
        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(any(), any())).willReturn(false);
        assertThrows(SecurityException.class, () -> userService.changePassword(email, req));
    }

    @Test
    void initiatePasswordReset_ShouldSendEmail() {
        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        given(tokenService.generatePasswordResetToken(user)).willReturn("resetToken");

        userService.initiatePasswordReset(email);

        then(emailService).should().sendPasswordResetEmail(email, "resetToken");
    }

    @Test
    void initiatePasswordReset_ShouldThrowNotFound_WhenMissing() {
        given(repository.findByEmail(email)).willReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.initiatePasswordReset(email));
    }

    @Test
    void completePasswordReset_ShouldResetPasswordAndRevokeTokens() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("validToken");
        req.setNewPassword("newPwd");
        given(tokenService.validatePasswordResetToken("validToken")).willReturn(email);
        given(repository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPwd")).willReturn("encodedNewPwd");

        userService.completePasswordReset(req);

        assertEquals("encodedNewPwd", user.getPassword());
        then(authService).should().revokeAllUserTokens(user);
        then(repository).should().save(user);
    }

    @Test
    void completePasswordReset_ShouldThrowWhenInvalidToken() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("invalid");
        given(tokenService.validatePasswordResetToken("invalid")).willThrow(new SecurityException("Invalid token"));
        assertThrows(SecurityException.class, () -> userService.completePasswordReset(req));
    }

    @Test
    void completePasswordReset_ShouldThrowWhenUserNotFound() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("validToken");
        req.setNewPassword("newPwd");
        given(tokenService.validatePasswordResetToken("validToken")).willReturn(email);
        given(repository.findByEmail(email)).willReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.completePasswordReset(req));
    }
}
