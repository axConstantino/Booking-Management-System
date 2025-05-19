package com.axconstantino.reservationsystem.bookingtest;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.database.repository.BookingRepository;
import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.booking.mapper.BookingMapper;
import com.axconstantino.reservationsystem.booking.service.BookingService;
import com.axconstantino.reservationsystem.common.exception.ConflictException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceUnitTest {

    @Mock BookingRepository bookingRepo;
    @Mock RoomRepository roomRepo;
    @Mock UserRepository userRepo;
    @Mock BookingMapper mapper;
    @InjectMocks BookingService bookingService;

    private Long roomId;
    private LocalDate start;
    private LocalDate end;
    private BookingRequest req;
    private User user;
    private Room room;

    @BeforeEach
    void setup() {
        roomId = 1L;
        start = LocalDate.of(2025, 6, 1);
        end   = LocalDate.of(2025, 6, 3);
        req = new BookingRequest(roomId, start, end);
        user = User.builder().email("u@e.com").build();
        room = Room.builder().id(roomId)
                .pricePerNight(new BigDecimal("100"))
                .build();
    }

    @Test
    void createBooking_success() {
        when(userRepo.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(roomRepo.findByIdWithLock(roomId)).thenReturn(Optional.of(room));
        when(bookingRepo.findByRoomAndDatesOverlap(room, start, end))
                .thenReturn(Collections.emptyList());
        when(bookingRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toDto(any())).thenReturn(
                new BookingResponse(UUID.randomUUID(), roomId, user.getId(), start, end,
                        room.getPricePerNight().doubleValue() * 2, null)
        );

        BookingResponse resp = bookingService.createBooking(req, "u@e.com");

        assertNotNull(resp);
        verify(bookingRepo).save(argThat(b ->
                b.getUser().equals(user)
                        && b.getRoom().equals(room)
                        && b.getTotalPrice().equals(new BigDecimal("200"))
        ));
        verify(mapper).toDto(any());
    }

    @Test
    void createBooking_userNotFound_throwsNotFoundException() {
        when(userRepo.findByEmail("not@exists.com"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> bookingService.createBooking(req, "not@exists.com"));
    }

    @Test
    void createBooking_roomNotAvailable_throwsConflictException() {
        when(userRepo.findByEmail("u@e.com")).thenReturn(Optional.of(user));
        when(roomRepo.findByIdWithLock(roomId)).thenReturn(Optional.of(room));
        when(bookingRepo.findByRoomAndDatesOverlap(room, start, end))
                .thenReturn(Collections.singletonList(mock(Booking.class)));

        assertThrows(ConflictException.class,
                () -> bookingService.createBooking(req, "u@e.com"));
    }

    @Test
    void calculateTotalPrice_variosDias_correcto() {
        LocalDate s = LocalDate.of(2025,5,20);
        LocalDate e = LocalDate.of(2025,5,25);
        Room r = Room.builder().pricePerNight(new BigDecimal("50")).id(1L).build();

        BigDecimal precio = bookingService.calculateTotalPrice(r, s, e);
        assertEquals(new BigDecimal("250"), precio);
    }
}
