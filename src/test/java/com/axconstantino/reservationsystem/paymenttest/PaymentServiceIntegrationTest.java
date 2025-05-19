package com.axconstantino.reservationsystem.paymenttest;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import com.axconstantino.reservationsystem.booking.database.repository.BookingRepository;
import com.axconstantino.reservationsystem.booking.dto.BookingRequest;
import com.axconstantino.reservationsystem.booking.dto.BookingResponse;
import com.axconstantino.reservationsystem.booking.service.BookingService;
import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.payment.dto.CheckoutSessionResponse;
import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.repository.RoomRepository;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@ExtendWith(SpringExtension.class)
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class PaymentServiceIntegrationTest {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;


    private User getTestUser() {
        return userRepository.findByEmail("user@test.com")
                .orElseThrow(() -> new NotFoundException("Test user not found in data.sql"));
    }

    private Room getAvailableRoom() {
        return roomRepository.findByName("Standard Room")
                .orElseThrow(() -> new NotFoundException("Test room not found in data.sql"));
    }

    @Test
    @Transactional
    void confirmBookingPayment_Integration_PersistsStatusChange() {
        Booking booking = bookingRepository.save(Booking.builder()
                .user(getTestUser())
                .room(getAvailableRoom())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(300))
                .bookingStatus(BookingStatus.PENDING)
                .build());

        paymentService.confirmBookingPayment(booking.getId());

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @Transactional
    void fullPaymentFlow_SimulateWebhookConfirmsBooking() throws StripeException {
        User user = getTestUser();
        Room room = getAvailableRoom();

        BookingRequest request = new BookingRequest(
                room.getId(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(4)
        );

        BookingResponse bookingResponse = bookingService.createBooking(request, user.getEmail());

        CheckoutSessionResponse stripeUrl = paymentService.createCheckoutSession(bookingResponse.getId(), user.getEmail());
        assertThat(stripeUrl).isNotNull();

        paymentService.confirmBookingPayment(bookingResponse.getId());

        Booking updatedBooking = bookingRepository.findById(bookingResponse.getId()).orElseThrow();
        assertThat(updatedBooking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @Transactional
    void createCheckoutSession_InvalidBooking_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () ->
                paymentService.createCheckoutSession(nonExistentId, "nonexistent@test.com"));
    }
}