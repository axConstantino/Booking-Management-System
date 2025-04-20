package com.axconstantino.reservationsystem.booking.controller;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.booking.mapper.BookingMapper;
import com.axconstantino.reservationsystem.booking.service.BookingService;
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

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody @Valid BookingRequest bookingRequest,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.createBooking(bookingRequest, userEmail);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getUserBookings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<Booking> bookings = bookingService.getUserBookings(userEmail);
        return new ResponseEntity<>(bookingMapper.toDtoList(bookings), HttpStatus.OK);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBookingDetails(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.getBookingDetails(bookingId, userEmail);
        return ResponseEntity.ok(response);
    }

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
