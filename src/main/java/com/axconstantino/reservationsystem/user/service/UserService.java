package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.auth.service.AuthService;
import com.axconstantino.reservationsystem.common.exception.DuplicateEntityException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.mail.EmailService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.dto.ChangePasswordRequest;
import com.axconstantino.reservationsystem.user.dto.ResetPasswordRequest;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.validation.PhoneValidator;
import lombok.extern.slf4j.Slf4j;
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
    private final EmailService emailService;
    private final AuthService authService;

    public UserService(UserRepository repository, UserMapper mapper, PhoneValidator phoneValidator, PasswordEncoder passwordEncoder, TokenService tokenService, EmailService emailService, AuthService authService) {
        super(repository, mapper);
        this.phoneValidator = phoneValidator;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
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
        log.debug("Attempting to fetch user by email: {}", email);
        return repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new NotFoundException("User not found with email: " + email);
                });
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
        log.info("Updating basic info for user: {}", email);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        mapper.updateFromDTO(user, updateRequest);

        if (updateRequest.getPhone() != null) {
            String formattedPhone = phoneValidator.formatToE164(updateRequest.getPhone());
            log.debug("Updating phone number for user {}: {}", email, formattedPhone);
            user.setPhone(formattedPhone);
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
        log.info("Adding role {} to user {}", newRole, userId);
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (user.getRoles().contains(newRole)) {
            log.warn("Duplicate role assignment attempt: {} for user {}", newRole, userId);
            throw new DuplicateEntityException("User already has role: " + newRole);
        }

        user.getRoles().add(newRole);
        log.debug("Revoking all tokens for user {}", userId);
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
    public User addPhone(String email, String phone) {
        log.info("Adding phone number to user: {}", email);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        if (user.getPhone() != null) {
            log.warn("Phone number already exists for user: {}", email);
            throw new IllegalStateException("User already has a registered phone number");
        }

        String formattedPhone = phoneValidator.formatToE164(phone);
        log.debug("Formatted phone number for {}: {}", email, formattedPhone);
        user.setPhone(formattedPhone);
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
        log.info("Password change requested for user: {}", email);
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Invalid current password attempt for user: {}", email);
            throw new SecurityException("Current password verification failed");
        }

        log.debug("Encoding new password for user: {}", email);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
        log.info("Password successfully updated for user: {}", email);
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
        log.info("Initiating password reset for: {}", email);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Password reset attempt for non-existent user: {}", email);
                    return new NotFoundException("User not found");
                });

        String resetToken = tokenService.generatePasswordResetToken(user);
        log.debug("Generated reset token for {}: {}", email, resetToken);
        emailService.sendPasswordResetEmail(email, resetToken);
        log.info("Password reset email sent to: {}", email);
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
        log.info("Processing password reset completion");
        String email = tokenService.validatePasswordResetToken(request.getToken());
        log.debug("Token validated for email: {}", email);

        User user = repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Valid token for non-existent user: {}", email);
                    return new NotFoundException("User not found");
                });

        log.debug("Encoding new password for {}", email);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
        log.info("Password successfully reset for user: {}", email);
        authService.revokeAllUserTokens(user);
        log.debug("All tokens revoked for user: {}", email);
    }

}