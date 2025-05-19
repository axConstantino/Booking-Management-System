package com.axconstantino.reservationsystem.bookingtest;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import com.axconstantino.reservationsystem.booking.database.repository.BookingRepository;
import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.booking.service.BookingService;
import com.axconstantino.reservationsystem.common.exception.ConflictException;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BookingServiceIntegrationTest {

    @Autowired private BookingService service;
    @Autowired private RoomRepository roomRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private BookingRepository bookingRepo;

    private final LocalDate start = LocalDate.of(2025, 7, 1);
    private final LocalDate end   = LocalDate.of(2025, 7, 4);

    @Test
    void whenCreateBooking_thenPersistedAndRetrievable() {
        User user = userRepo.findByEmail("user@test.com")
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Room room = roomRepo.findByName("Standard Room")
                .orElseThrow(() -> new IllegalStateException("Room not found"));

        BookingRequest req = new BookingRequest(room.getId(), start, end);
        BookingResponse response = service.createBooking(req, user.getEmail());

        var booking = bookingRepo.findById(response.getId())
                .orElseThrow(() -> new IllegalStateException("Booking not found"));

        assertEquals(user.getEmail(), booking.getUser().getEmail());
        assertEquals(room.getPricePerNight(), booking.getRoom().getPricePerNight());
    }

    @Test
    void transactionalRollbackOnConflict() {
        User user = userRepo.findByEmail("user@test.com")
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Room room = roomRepo.findByName("Standard Room")
                .orElseThrow(() -> new IllegalStateException("Room not found"));

        BookingRequest firstBooking = new BookingRequest(room.getId(), start, end);
        service.createBooking(firstBooking, user.getEmail());

        BookingRequest conflictingBooking = new BookingRequest(
                room.getId(),
                start.plusDays(1),
                end.plusDays(1)
        );

        assertThrows(ConflictException.class,
                () -> service.createBooking(conflictingBooking, user.getEmail()));

        long count = bookingRepo.count();
        assertEquals(2, count);
    }
}