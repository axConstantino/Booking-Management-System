package com.axconstantino.reservationsystem.booking.service;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import com.axconstantino.reservationsystem.booking.database.repository.BookingRepository;
import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.booking.mapper.BookingMapper;
import com.axconstantino.reservationsystem.common.exception.ConflictException;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BookingService extends BaseCRUDService<Booking, BookingResponse, UUID, BookingRepository, BookingMapper> {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository repository, BookingMapper mapper, RoomRepository roomRepository, UserRepository userRepository) {
        super(repository, mapper);
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest bookingRequest, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + userEmail));

        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + bookingRequest.getRoomId()));

        checkRoomAvailability(room, bookingRequest.getStartDate(), bookingRequest.getEndDate());

        double totalPrice = calculateTotalPrice(room, bookingRequest.getStartDate(), bookingRequest.getEndDate());

        Booking booking = Booking.builder()
                .room(room)
                .user(user)
                .startDate(bookingRequest.getStartDate())
                .endDate(bookingRequest.getEndDate())
                .totalPrice(totalPrice)
                .bookingStatus(BookingStatus.PENDING)
                .build();

        Booking savedBooking = repository.save(booking);
        return mapper.toDto(savedBooking);
    }


    private void checkRoomAvailability(Room room, LocalDateTime startDate, LocalDateTime endDate) {
        boolean isAvailable = repository.findByRoomAndDatesOverlap(room, startDate, endDate).isEmpty();
        if (!isAvailable) {
            throw new ConflictException("Room is not available for the selected dates");
        }
    }

    private double calculateTotalPrice(Room room, LocalDateTime startDate, LocalDateTime endDate) {
        long nights = Duration.between(startDate, endDate).toDays();
        return room.getPricePerNight() * nights;
    }
}
