package com.axconstantino.reservationsystem.booking.controller;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.booking.mapper.BookingMapper;
import com.axconstantino.reservationsystem.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Validated
public class BookingUserController {
    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @Operation(summary = "Create a booking", description = "Creates a new booking for a specific room and user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking created successfully", content = @Content(schema = @Schema(implementation = BookingResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or Room not found"),
            @ApiResponse(responseCode = "409", description = "Room is not available for the selected dates")
    })
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody @Valid BookingRequest bookingRequest,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.createBooking(bookingRequest, userEmail);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get bookings for user", description = "Returns all bookings for the given user")
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getUserBookings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<Booking> bookings = bookingService.getUserBookings(userEmail);
        return new ResponseEntity<>(bookingMapper.toDtoList(bookings), HttpStatus.OK);
    }

    @Operation(summary = "Get a booking by ID", description = "Returns booking details for a given ID and user email")
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBookingDetails(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.getBookingDetails(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update a booking", description = "Modifies a booking in PENDING status")
    @PutMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable UUID bookingId,
            @RequestBody @Valid BookingRequest bookingRequest,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.updateBooking(bookingId, bookingRequest, userEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel a booking", description = "Cancels a booking for the current user")
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        bookingService.cancelBooking(bookingId, userEmail);
        return ResponseEntity.noContent().build();
    }
}
