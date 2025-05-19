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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service class for handling booking-related operations.
 * Inherits basic CRUD functionality from {@link BaseCRUDService} and adds custom logic for:
 * booking creation, updating, cancellation, and user-specific queries.
 */
@Slf4j
@Service
public class BookingService extends BaseCRUDService<Booking, BookingResponse, UUID, BookingRepository, BookingMapper> {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /**
     * Constructs a new instance of {@code BookingService} with required dependencies.
     *
     * @param bookingRepository the booking bookingRepository
     * @param mapper          the booking mapper
     * @param roomRepository  the room bookingRepository
     * @param userRepository  the user bookingRepository
     */
    public BookingService(BookingRepository bookingRepository, BookingMapper mapper, RoomRepository roomRepository, UserRepository userRepository) {
        super(bookingRepository, mapper);
        this.bookingRepository = repository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new booking for a given user and request.
     *
     * @param bookingRequest the booking details
     * @param userEmail      the email of the user making the booking
     * @return the created booking as a DTO
     * @throws NotFoundException    if the user or room is not found
     * @throws ConflictException    if the room is not available for the given dates
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest bookingRequest, String userEmail) {
        log.info("Attempting to create booking for user: {}", userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", userEmail);
                    return new NotFoundException("User not found with email: " + userEmail);
                });

        Room room = roomRepository.findByIdWithLock(bookingRequest.getRoomId())
                .orElseThrow(() -> {
                    log.warn("Room not found with id: {}", bookingRequest.getRoomId());
                    return new NotFoundException("Room not found with id: " + bookingRequest.getRoomId());
                });

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

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking created with ID: {}", savedBooking.getId());
        return mapper.toDto(savedBooking);
    }

    /**
     * Retrieves a booking entity by its ID.
     *
     * @param bookingId the ID of the booking
     * @return the booking entity
     * @throws NotFoundException if the booking is not found
     */
    public Booking findBookingById(UUID bookingId) {
        log.debug("Fetching booking by ID: {}", bookingId);
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found with ID: {}", bookingId);
                    return new NotFoundException("Booking not found with id: " + bookingId);
                });
    }

    /**
     * Retrieves all bookings made by a specific user.
     *
     * @param userEmail the email of the user
     * @return a list of the user's bookings
     * @throws NotFoundException if the user is not found
     */
    public List<Booking> getUserBookings(String userEmail) {
        log.debug("Fetching bookings for user: {}", userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", userEmail);
                    return new NotFoundException("User not found with email: " + userEmail);
                });

        List<Booking> bookings = bookingRepository.findByUser(user);
        log.info("Found {} bookings for user: {}", bookings.size(), userEmail);
        return bookings;
    }

    /**
     * Finds a booking by its ID and the user who created it.
     *
     * @param bookingId the ID of the booking
     * @param userEmail the email of the user
     * @return the booking entity
     * @throws NotFoundException if the booking or user is not found
     */
    public Booking findByIdAndUser(UUID bookingId, String userEmail) {
        log.info("Finding booking by ID: {} for user: {}", bookingId, userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", userEmail);
                    return new NotFoundException("User not found with email: " + userEmail);
                });

        return bookingRepository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> {
                    log.warn("Booking not found with ID: {} for user: {}", bookingId, userEmail);
                    return new NotFoundException("Booking not found with id: " + bookingId);
                });
    }

    /**
     * Retrieves booking details for a user, ensuring ownership.
     *
     * @param bookingId the ID of the booking
     * @param userEmail the email of the user
     * @return the booking details as a DTO
     * @throws NotFoundException if the booking or user is not found or mismatched
     */
    @Transactional
    public BookingResponse getBookingDetails(UUID bookingId, String userEmail) {
        log.debug("Retrieving booking details for bookingId: {} and userEmail: {}", bookingId, userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", userEmail);
                    return new NotFoundException("User not found with email: " + userEmail);
                });

        Booking booking = bookingRepository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> {
                    log.warn("Booking not found with ID: {} for user: {}", bookingId, userEmail);
                    return new NotFoundException("Booking not found with id: " + bookingId);
                });

        if (!booking.getUser().equals(user)) {
            log.warn(" User {} does not own booking {}", userEmail, bookingId);
            throw new NotFoundException("Booking not found for the user");
        }

        return mapper.toDto(booking);
    }

    /**
     * Updates an existing booking if it is in PENDING status.
     *
     * @param bookingId     the ID of the booking to update
     * @param updateRequest the updated booking information
     * @param userEmail     the email of the user making the update
     * @return the updated booking as a DTO
     * @throws NotFoundException if the user, booking, or room is not found
     * @throws ConflictException if the booking is not in PENDING status or dates conflict
     */
    @Transactional
    public BookingResponse updateBooking(UUID bookingId, BookingRequest updateRequest, String userEmail) {
        log.info("Attempting to update booking {} for user {}", bookingId, userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", userEmail);
                    return new NotFoundException("User not found");
                });

        Booking booking = bookingRepository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> {
                    log.warn("Booking not found with ID: {} for user: {}", bookingId, userEmail);
                    return new NotFoundException("Booking not found");
                });

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            log.warn("Attempt to update non-pending booking {} by user {}", bookingId, userEmail);
            throw new ConflictException("Only reservations in PENDING status can be modified.");
        }

        Room room = roomRepository.findById(updateRequest.getRoomId())
                .orElseThrow(() -> {
                    log.warn("Room not found with ID: {}", updateRequest.getRoomId());
                    return new NotFoundException("Room not found");
                });

        checkRoomAvailability(room, updateRequest.getStartDate(), updateRequest.getEndDate());

        booking.setStartDate(updateRequest.getStartDate());
        booking.setEndDate(updateRequest.getEndDate());
        booking.setTotalPrice(calculateTotalPrice(room, updateRequest.getStartDate(), updateRequest.getEndDate()));

        Booking updatedBooking = bookingRepository.save(booking);

        log.info("Booking {} updated successfully", bookingId);
        return mapper.toDto(updatedBooking);
    }

    /**
     * Cancel a reservation by changing its status to CANCELLED.
     * @param bookingId the ID of the booking to cancel
     * @param userEmail the email of the user cancelling the booking
     * @throws NotFoundException if the user or booking is not found
     * @throws ConflictException if the booking is already cancelled
     */
    @Transactional
    public void cancelBooking(UUID bookingId, String userEmail) {
        log.info("Attempting to cancel booking {} for user {}", bookingId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", userEmail);
                    return new NotFoundException("User not found");
                });

        Booking booking = bookingRepository.findByIdAndUser(bookingId, user)
                .orElseThrow(() -> {
                    log.warn("Booking not found with ID: {} for user: {}", bookingId, userEmail);
                    return new NotFoundException("Booking not found");
                });

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            log.warn("Attempt to cancel already cancelled booking: {}", bookingId);
            throw new ConflictException("Booking is already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully", bookingId);
    }

    /**
     * Checks whether a room is available for the given date range.
     *
     * @param room       the room to check
     * @param startDate  the start date of the booking
     * @param endDate    the end date of the booking
     * @throws ConflictException if there is a date conflict with existing bookings
     */
    private void checkRoomAvailability(Room room, LocalDate startDate, LocalDate endDate) {
        log.debug("Checking availability for room {} from {} to {}", room.getId(), startDate, endDate);
        boolean isAvailable = bookingRepository.findByRoomAndDatesOverlap(room, startDate, endDate).isEmpty();
        if (!isAvailable) {
            log.warn("Room {} is not available from {} to {}", room.getId(), startDate, endDate);
            throw new ConflictException("Room is not available for the selected dates");
        }
    }

    /**
     * Calculates the total price for a booking based on the room's nightly rate and the number of nights
     * between the start and end dates.
     * <p>
     * The booking must be at least one night. If the {@code endDate} is not after {@code startDate},
     * an {@link IllegalArgumentException} is thrown.
     *</p>
     * @param room      the room being booked; must not be {@code null}
     * @param startDate the check-in date (inclusive); must not be {@code null}
     * @param endDate   the check-out date (exclusive); must be after {@code startDate} and not {@code null}
     * @return the total booking price as a {@link BigDecimal}
     * @throws IllegalArgumentException if {@code endDate} is not after {@code startDate}
     */
    public BigDecimal calculateTotalPrice(Room room, LocalDate startDate, LocalDate endDate) {
        long nights = ChronoUnit.DAYS.between(startDate, endDate);

        if (nights <= 0) {
            throw new IllegalArgumentException("The departure date must be after the arrival date.");
        }

        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        log.debug("Calculated total price {} for room {} for {} nights", totalPrice, room.getId(), nights);
        return totalPrice;
    }

}
