package com.axconstantino.reservationsystem.admin;

import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomUpdateDTO;
import com.axconstantino.reservationsystem.rooms.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequestMapping("admin/rooms")
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin - Room Management", description = "Endpoints for managing hotel rooms by admins")
public class RoomAdminController {
    private final RoomService roomService;

    @Operation(
            summary = "Get all rooms (paginated)",
            description = "Returns a paginated list of rooms sorted by name in descending order."
    )
    @GetMapping
    public ResponseEntity<Page<RoomDTO>> getRooms(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RoomDTO> rooms = roomService.getAll(pageable);
        return ResponseEntity.ok(rooms);
    }

    @Operation(
            summary = "Create a new room",
            description = "Creates a new room using the provided RoomDTO."
    )
    @ApiResponse(responseCode = "200", description = "Room successfully created")
    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomDTO newRoom) {
        RoomDTO savedRoom = roomService.create(newRoom);
        return ResponseEntity.ok(savedRoom);
    }

    @Operation(
            summary = "Update an existing room",
            description = "Updates an existing room using the provided data."
    )
    @ApiResponse(responseCode = "200", description = "Room successfully updated")
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomUpdateDTO updateRequest) {
        RoomDTO updatedRoom = roomService.updateRoom(roomId, updateRequest);
        return ResponseEntity.ok(updatedRoom);
    }


    @Operation(
            summary = "Upload images for a room",
            description = "Uploads one or more images to associate them with a specific room."
    )
    @ApiResponse(responseCode = "200", description = "Images uploaded successfully")
    @ApiResponse(responseCode = "500", description = "Error uploading images", content = @Content)
    @PostMapping("/{roomId}/images")
    public ResponseEntity<?> uploadRoomImages(
            @PathVariable Long roomId,
            @RequestParam("images") MultipartFile[] images
    ) {
        try {
            roomService.uploadImages(roomId, images);
            return ResponseEntity.ok("Images uploaded successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading images.");
        }

    }

    @Operation(
            summary = "Delete a room",
            description = "Deletes a room by its ID."
    )
    @ApiResponse(responseCode = "204", description = "Room successfully deleted")
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        roomService.delete(roomId);
        return ResponseEntity.noContent().build();
    }
}
