package com.axconstantino.reservationsystem.admin;

import com.axconstantino.reservationsystem.user.database.model.enums.Role;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/users")
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - User Management", description = "Endpoints for managing users by administrators")
public class UserAdminController {
    private final UserService service;

    @Operation(
            summary = "Get all users (paginated)",
            description = "Returns a paginated list of all users, sorted by name in ascending order."
    )
    @ApiResponse(responseCode = "200", description = "List of users retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC)Pageable pageable
    ) {
        Page<UserDTO> usersPage = service.getAll(pageable);
        return ResponseEntity.ok(usersPage);
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user by their UUID."
    )
    @ApiResponse(responseCode = "200", description = "User found and returned")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID userId) {
        return service.get(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Add role to user",
            description = "Assigns a new role to an existing user."
    )
    @ApiResponse(responseCode = "204", description = "Role added successfully")
    @PutMapping("/{userId}")
    public ResponseEntity<Void> addUserRole(
            @PathVariable UUID userId,
            @RequestParam Role role
    ) {
        service.addUserRole(userId, role);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete user",
            description = "Deletes a user by their UUID."
    )
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        service.delete(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
