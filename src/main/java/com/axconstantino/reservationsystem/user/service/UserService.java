package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.auth.service.AuthService;
import com.axconstantino.reservationsystem.common.exception.DuplicateEntityException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.dto.ChangePasswordRequest;
import com.axconstantino.reservationsystem.user.dto.ResetPasswordRequest;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.validation.PhoneValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service extends basic CRUD operations for {@link User} entities.
 * and providing additional user-specific user behaviors such as:
 * <ul>
 *     <li>Retrieval by email.</li>
 *     <li>Updating basic profile information.</li>
 *     <li>Role management.</li>
 *     <li>Phone number addition.</li>
 *     <li>Password checks and reset workflows.</li>
 * </ul>
 */
@Slf4j
@Service
public class UserService extends BaseCRUDService<User, UserDTO, UUID, UserRepository, UserMapper> {
    private final PhoneValidator phoneValidator;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JavaMailSender mailSender;
    private final AuthService authService;

    public UserService(UserRepository repository, UserMapper mapper, PhoneValidator phoneValidator, PasswordEncoder passwordEncoder, TokenService tokenService, JavaMailSender mailSender, AuthService authService) {
        super(repository, mapper);
        this.phoneValidator = phoneValidator;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailSender = mailSender;
        this.authService = authService;
    }

    /**
     * Retrieves a {@link User} by their email address.
     *
     * @param email the email of the user to retrieve
     * @return the {@link User} with the given email
     * @throws NotFoundException if no user exists with that email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));
    }

    /**
     * Updates a user's basic profile information (name, address, etc.)
     * and optionally formats and stores a new phone number.
     *
     * @param email         the email of the user to update
     * @param updateRequest a DTO containing new field values
     * @return the updated {@link User} entity
     * @throws NotFoundException if no user exists with that email
     */
    @Transactional
    public User updateUserBasicInfo(String email, UserDTO updateRequest) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));

        mapper.updateFromDTO(user, updateRequest);

        if (updateRequest.getPhone() != null) {
            user.setPhone(phoneValidator.formatToE164(updateRequest.getPhone()));
        }

        return repository.save(user);
    }

    /**
     * Adds a new role to the specified user, revokes existing authentication tokens,
     * and persists the change.
     *
     * @param userId  the UUID of the user
     * @param newRole the {@link Role} to add
     * @throws NotFoundException        if the user does not exist
     * @throws DuplicateEntityException if the user already has the role
     */
    @Transactional
    public void addUserRole(UUID userId, Role newRole) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getRoles().contains(newRole)) {
            throw new DuplicateEntityException("The user already has this role");
        }

        user.getRoles().add(newRole);
        authService.revokeAllUserTokens(user);
        repository.save(user);
    }

    /**
     * Adds a phone number to the user’s profile, formatted to E.164.
     *
     * @param email the email of the user
     * @param phone the raw phone number string
     * @return the updated {@link User} entity
     * @throws NotFoundException    if no user exists with that email
     * @throws IllegalStateException if the user already has a phone number
     */
    @Transactional
    public User addPhone(String email, String phone) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));

        if (user.getPhone() != null) {
            throw new RuntimeException("You already have a phone number");
        }

        user.setPhone(phoneValidator.formatToE164(phone));
        return repository.save(user);
    }

    /**
     * Changes the user’s password after validating the current one.
     *
     * @param email   the email of the user
     * @param request DTO containing current and new password
     * @throws NotFoundException  if the user does not exist
     * @throws SecurityException if the current password is incorrect
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new SecurityException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    /**
     * Initiates the password reset process by generating a token
     * and sending it via email.
     *
     * @param email the email of the user requesting reset
     * @throws NotFoundException if no user exists with that email
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        String resetToken = tokenService.generatePasswordResetToken(user);
        sendResetEmail(user.getEmail(), resetToken);
    }

    /**
     * Sends a password reset email containing the raw reset token.
     *
     * @param email the recipient’s email address
     * @param token the raw password reset token
     */
    private void sendResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Request");
        message.setText("Your password reset token: " + token);
        mailSender.send(message);
    }

    /**
     * Completes the password reset by validating the provided token,
     * encoding the new password, and saving the user.
     *
     * @param request DTO containing the reset token and new password
     * @throws SecurityException if the token is invalid or expired
     * @throws NotFoundException if no user exists for the validated token
     */
    @Transactional
    public void completePasswordReset(ResetPasswordRequest request) {
        String email = tokenService.validatePasswordResetToken(request.getToken());
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

}