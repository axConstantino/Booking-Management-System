package com.axconstantino.reservationsystem.user.controller;

import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.dto.ChangePasswordRequest;
import com.axconstantino.reservationsystem.user.dto.ResetPasswordRequest;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RequestMapping("/users/me")
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "User", description = "User management and authentication")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(
            summary = "Get current user",
            description = "Retrieve authenticated user's information"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        if (user != null) {
            return ResponseEntity.ok(userMapper.toDto(user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
            summary = "Update user",
            description = "Update basic user information"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ResponseEntity<UserDTO> updateCurrentUser(@RequestBody @Valid UserDTO updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User updatedUser = userService.updateUserBasicInfo(email, updateRequest);
        if (updatedUser != null) {
            return ResponseEntity.ok(userMapper.toDto(updatedUser));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
            summary = "Add phone number",
            description = "Add phone number to user profile"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Phone number added"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<UserDTO> addPhone(@Valid @RequestBody UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User updatedUser = userService.addPhone(email, userDTO.getPhone());
        if (updatedUser != null) {
            return ResponseEntity.ok(userMapper.toDto(updatedUser));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
            summary = "Change password",
            description = "Update authenticated user's password"
    )
    @ApiResponse(responseCode = "204", description = "Password updated")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        userService.changePassword(userEmail, request);
        return ResponseEntity.noContent().build();
    }


    @Operation(
            summary = "Request password reset",
            description = "Initiate password recovery process"
    )
    @ApiResponse(responseCode = "204", description = "Reset request processed")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> requestPasswordReset(
            @RequestParam String email) {
        userService.initiatePasswordReset(email);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Confirm password reset",
            description = "Complete password recovery process"
    )
    @ApiResponse(responseCode = "204", description = "Password reset completed")
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.completePasswordReset(request);
        return ResponseEntity.noContent().build();
    }

}
