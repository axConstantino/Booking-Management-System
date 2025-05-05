package com.axconstantino.reservationsystem.rooms.controller;

import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import com.axconstantino.reservationsystem.rooms.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
@Validated
@Tag(name = "Rooms", description = "Operations related to hotel rooms")
public class RoomController {

    private final RoomService service;

    @Operation(summary = "Get all rooms", description = "Retrieves a paginated and sorted list of rooms.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rooms list retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoomDTO.class)))
    })
    @GetMapping
    public ResponseEntity<Page<RoomDTO>> getAllRooms(
            @Parameter(description = "Pagination parameters")
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @Operation(summary = "Get room by ID", description = "Retrieves details of a specific room by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoomDTO.class))),
            @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    })
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoomById(
            @Parameter(description = "ID of the room to retrieve", required = true)
            @PathVariable Long roomId
    ) {
        return service.get(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search available rooms",
            description = "Filters and returns a paginated list of available rooms based on search criteria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available rooms list retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RoomDTO.class)))
    })
    @GetMapping("/search-available-room")
    public ResponseEntity<Page<RoomDTO>> searchAvailableRooms(
            @Parameter(description = "Room filter criteria", required = true)
            @Valid @ModelAttribute RoomFilterRequest filterRequest
    ) {
        return ResponseEntity.ok(service.getAvailableRooms(filterRequest));
    }
}
