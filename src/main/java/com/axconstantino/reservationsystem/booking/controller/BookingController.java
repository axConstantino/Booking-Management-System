package com.axconstantino.reservationsystem.booking.controller;

import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Validated
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody @Valid BookingRequest bookingRequest,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.createBooking(bookingRequest, userEmail);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
