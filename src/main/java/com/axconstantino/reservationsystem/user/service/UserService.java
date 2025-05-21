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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service layer for managing {@link User} entities, extending basic CRUD operations
 * provided by {@link BaseCRUDService}. This service encapsulates user-specific
 * business logic such as:
 * <ul>
 *     <li>Retrieving users by email.</li>
 *     <li>Updating basic user profile information including phone number formatting and validation.</li>
 *     <li>Managing user roles with role addition and validation.</li>
 *     <li>Handling secure password operations including change, reset initiation, and completion workflows.</li>
 *     <li>Revoking authentication tokens upon sensitive security changes.</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Methods are transactional and designed for use in concurrent environments.</p>
 *
 * <p><b>Logging:</b> Uses SLF4J for detailed method entry, success, and error logging to facilitate
 * troubleshooting and audit trails.</p>
 *
 * <p><b>Exception Handling:</b> Throws domain-specific exceptions such as {@link NotFoundException} and
 * {@link DuplicateEntityException} to signal error conditions clearly.</p>
 */
@Slf4j
@Service
public class UserService extends BaseCRUDService<User, UserDTO, UUID, UserRepository, UserMapper> {

    private final PhoneValidator phoneValidator;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final AuthService authService;

    /**
     * Constructs a {@code UserService} with required dependencies.
     *
     * @param repository      repository for user persistence operations
     * @param mapper          mapper for converting between User and UserDTO
     * @param phoneValidator  utility to validate and format phone numbers
     * @param passwordEncoder encoder for securely hashing passwords
     * @param tokenService    service for generating and validating security tokens
     * @param emailService    service for sending notification emails
     * @param authService     service for managing authentication tokens and sessions
     */
    public UserService(UserRepository repository,
                       UserMapper mapper,
                       PhoneValidator phoneValidator,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       EmailService emailService,
                       AuthService authService) {
        super(repository, mapper);
        this.phoneValidator = phoneValidator;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.authService = authService;
    }

    /**
     * Retrieves a {@link User} entity by their unique email address.
     *
     * @param email the email of the user to retrieve; must be non-null and valid format
     * @return the user entity matching the given email
     * @throws NotFoundException if no user exists with the provided email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("No user found with email: {}", email);
                    return new NotFoundException("User not found with email: " + email);
                });
    }

    /**
     * Updates the basic profile information of a user, including optional phone number formatting.
     * Fields such as name, address, etc., are updated via the provided DTO.
     *
     * <p>If the phone number is provided, it is validated and normalized to the E.164 format.</p>
     *
     * @param email         the email of the user to update; must exist in the system
     * @param updateRequest DTO containing the fields to be updated
     * @return the updated {@link User} entity
     * @throws NotFoundException if no user exists with the given email
     */
    @Transactional
    public User updateUserBasicInfo(String email, UserDTO updateRequest) {
        log.info("Updating basic profile info for user: {}", email);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        // Map non-null fields from DTO to entity, consider partial updates
        mapper.updateFromDTO(user, updateRequest);

        if (updateRequest.getPhone() != null) {
            String formattedPhone = phoneValidator.formatToE164(updateRequest.getPhone());
            log.debug("Formatted and updating phone number for user {}: {}", email, formattedPhone);
            user.setPhone(formattedPhone);
        }

        return repository.save(user);
    }

    /**
     * Updates a user's email address and refreshes the security update timestamp if applicable.
     *
     * @param userId   UUID identifier of the user
     * @param newEmail new email to assign to the user
     * @throws NotFoundException if no user exists with the given ID
     */
    @Transactional
    public void updateEmail(UUID userId, String newEmail) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        updateSecurityUpdatedAtIfNeeded(user, newEmail, null, null);
        user.setEmail(newEmail);

        repository.save(user);
    }

    /**
     * Adds a new {@link Role} to the user's set of roles after validating it is not already present.
     * Revokes all active authentication tokens for the user to ensure updated permissions take effect.
     *
     * @param userId  UUID of the user to update
     * @param newRole role to add to the user
     * @throws NotFoundException        if the user does not exist
     * @throws DuplicateEntityException if the user already has the specified role
     */
    @Transactional
    public void addUserRole(UUID userId, Role newRole) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (user.getRoles().contains(newRole)) {
            throw new DuplicateEntityException("User already has role: " + newRole);
        }

        Set<Role> updatedRoles = new HashSet<>(user.getRoles());
        updatedRoles.add(newRole);

        updateSecurityUpdatedAtIfNeeded(user, null, null, updatedRoles);
        user.setRoles(updatedRoles);

        authService.revokeAllUserTokens(user);
        repository.save(user);
    }

    /**
     * Adds a phone number to the user's profile after formatting it to E.164 standard.
     * Throws an exception if the user already has a phone number assigned.
     *
     * @param email the email of the user
     * @param phone raw phone number string to be validated and formatted
     * @return the updated user entity with the new phone number
     * @throws NotFoundException    if no user is found with the provided email
     * @throws IllegalStateException if the user already has a phone number
     */
    public User addPhone(String email, String phone) {
        log.info("Adding phone number to user profile: {}", email);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));

        if (user.getPhone() != null) {
            log.warn("Attempted to add phone to user who already has one: {}", email);
            throw new IllegalStateException("User already has a registered phone number");
        }

        String formattedPhone = phoneValidator.formatToE164(phone);
        log.debug("Phone number formatted for {}: {}", email, formattedPhone);
        user.setPhone(formattedPhone);

        return repository.save(user);
    }

    /**
     * Changes the password of the user after verifying the provided current password.
     * Updates the user's password with the encoded new password and refreshes security metadata.
     *
     * @param email   the email of the user requesting the password change
     * @param request contains current and new password information
     * @throws NotFoundException  if no user exists with the given email
     * @throws SecurityException  if the current password verification fails
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        log.info("Password change requested for user: {}", email);
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Incorrect current password provided for user: {}", email);
            throw new SecurityException("Current password verification failed");
        }

        log.debug("Encoding and updating password for user: {}", email);
        String newPasswordEncoded = passwordEncoder.encode(request.getNewPassword());
        updateSecurityUpdatedAtIfNeeded(user, null, newPasswordEncoded, null);
        user.setPassword(newPasswordEncoded);
        repository.save(user);

        log.info("Password successfully changed for user: {}", email);
    }

    /**
     * Initiates the password reset workflow by generating a reset token and
     * dispatching it via email to the user.
     *
     * @param email email address of the user requesting password reset
     * @throws NotFoundException if no user exists with the given email
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Starting password reset process for user: {}", email);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Password reset requested for unknown user: {}", email);
                    return new NotFoundException("User not found");
                });

        String resetToken = tokenService.generatePasswordResetToken(user);
        log.debug("Generated password reset token for user {}: {}", email, resetToken);
        emailService.sendPasswordResetEmail(email, resetToken);

        log.info("Password reset email sent to: {}", email);
    }

    /**
     * Completes the password reset process by validating the token,
     * encoding the new password, saving the user entity, and revoking
     * all existing authentication tokens to enforce security.
     *
     * @param request DTO containing reset token and new password
     * @throws SecurityException if the reset token is invalid or expired
     * @throws NotFoundException if no user corresponds to the validated token
     */
    @Transactional
    public void completePasswordReset(ResetPasswordRequest request) {
        log.info("Completing password reset");
        String email = tokenService.validatePasswordResetToken(request.getToken());
        log.debug("Password reset token validated for email: {}", email);

        User user = repository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Valid password reset token corresponds to non-existent user: {}", email);
                    return new NotFoundException("User not found");
                });

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        repository.save(user);

        authService.revokeAllUserTokens(user);

        log.info("Password reset completed and tokens revoked for user: {}", email);
    }

    /**
     * Updates the {@code securityUpdatedAt} timestamp on the user entity
     * if any security-sensitive attributes such as email, password, or roles
     * have changed, to signal downstream security systems of the update.
     *
     * @param user               the user entity to update
     * @param newEmail           the proposed new email address (nullable)
     * @param newPasswordEncoded the proposed new encoded password (nullable)
     * @param newRoles           the proposed new set of roles (nullable)
     */
    private void updateSecurityUpdatedAtIfNeeded(User user,
                                                 String newEmail,
                                                 String newPasswordEncoded,
                                                 Set<Role> newRoles) {
        boolean sensitiveChange = false;

        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            sensitiveChange = true;
        }

        if (newPasswordEncoded != null && !newPasswordEncoded.equals(user.getPassword())) {
            sensitiveChange = true;
        }

        if (newRoles != null && !newRoles.equals(user.getRoles())) {
            sensitiveChange = true;
        }

        if (sensitiveChange) {
            user.setSecurityUpdatedAt(LocalDateTime.now());
        }
    }
}
