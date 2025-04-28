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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService extends BaseCRUDService<Booking, BookingResponse, UUID, BookingRepository, BookingMapper> {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final BookingRepository repository;

    public BookingService(BookingRepository repository, BookingMapper mapper, RoomRepository roomRepository, UserRepository userRepository) {
        super(repository, mapper);
        this.repository = repository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest bookingRequest, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + userEmail));

        Room room = roomRepository.findByIdWithLock(bookingRequest.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found with id: " + bookingRequest.getRoomId()));

        checkRoomAvailability(room, bookingRequest.getStartDate(), bookingRequest.getEndDate());

        BigDecimal totalPrice = calculateTotalPrice(room, bookingRequest.getStartDate(), bookingRequest.getEndDate());

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

    public Booking findBookingById(UUID bookingId) {
        return repository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));
    }

    public List<Booking> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + userEmail));

        return repository.findByUser(user);
    }

    public Booking findByIdAndUser(UUID bookingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + userEmail));

        return repository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));
    }

    @Transactional
    public BookingResponse getBookingDetails(UUID bookingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + userEmail));

        Booking booking = repository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().equals(user)) {
            throw new NotFoundException("Booking not found for the user");
        }

        return mapper.toDto(booking);
    }

    @Transactional
    public BookingResponse updateBooking(UUID bookingId, BookingRequest updateRequest, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Booking booking = repository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Only reservations in PENDING status can be modified.");
        }

        Room room = roomRepository.findById(updateRequest.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found"));

        checkRoomAvailability(room, updateRequest.getStartDate(), updateRequest.getEndDate());

        booking.setStartDate(updateRequest.getStartDate());
        booking.setEndDate(updateRequest.getEndDate());
        booking.setTotalPrice(calculateTotalPrice(room, updateRequest.getStartDate(), updateRequest.getEndDate()));

        Booking updatedBooking = repository.save(booking);
        return mapper.toDto(updatedBooking);
    }

    @Transactional
    public void cancelBooking(UUID bookingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Booking booking = repository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking is already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        repository.save(booking);
    }

    private void checkRoomAvailability(Room room, LocalDateTime startDate, LocalDateTime endDate) {
        boolean isAvailable = repository.findByRoomAndDatesOverlap(room, startDate, endDate).isEmpty();
        if (!isAvailable) {
            throw new ConflictException("Room is not available for the selected dates");
        }
    }

    private BigDecimal calculateTotalPrice(Room room, LocalDateTime startDate, LocalDateTime endDate) {
        long hours = Duration.between(startDate, endDate).toHours();
        long nights = (hours + 23) / 24;
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }
}
