package com.axconstantino.reservationsystem.paymenttest;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import com.axconstantino.reservationsystem.booking.service.BookingService;
import com.axconstantino.reservationsystem.payment.dto.CheckoutSessionResponse;
import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class PaymentServiceUnitTest {

    @MockitoBean
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;


    @Test
    void confirmBookingPayment_WhenPending_UpdatesStatusInternally() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setBookingStatus(BookingStatus.PENDING);

        when(bookingService.findBookingById(bookingId)).thenReturn(booking);

        paymentService.confirmBookingPayment(bookingId);

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void createCheckoutSession_ValidBooking_IncludesCorrectMetadata() throws StripeException {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setTotalPrice(BigDecimal.valueOf(1500));

        Room room = new Room();
        room.setId(123L);
        room.setName("Suite");
        booking.setRoom(room);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        booking.setUser(user);

        when(bookingService.findByIdAndUser(bookingId, "test@test.com"))
                .thenReturn(booking);

        String expectedUrl = "http://localhost:8080/payment/success?bookingId=" + bookingId;
        String fakeSessionId = "cs_test_123";

        try (MockedStatic<Session> stripeSessionMock = Mockito.mockStatic(Session.class)) {
            Session fakeSession = new Session();
            fakeSession.setUrl(expectedUrl);
            fakeSession.setId(fakeSessionId);

            stripeSessionMock
                    .when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(fakeSession);

            CheckoutSessionResponse response = paymentService.createCheckoutSession(bookingId, "test@test.com");

            assertThat(response)
                    .as("The answer must not be null")
                    .isNotNull();

            assertThat(response.url())
                    .as("Must return the URL that Stripe returned")
                    .isEqualTo(expectedUrl);

            assertThat(response.sessionId())
                    .as("Must return the sessionId provided by Stripe")
                    .isEqualTo(fakeSessionId);
        }
    }

}
