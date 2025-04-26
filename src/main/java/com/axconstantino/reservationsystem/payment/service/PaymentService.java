package com.axconstantino.reservationsystem.payment.service;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.booking.database.model.enums.BookingStatus;
import com.axconstantino.reservationsystem.booking.service.BookingService;
import com.axconstantino.reservationsystem.common.exception.ConflictException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private String stripeSecretKey;
    private String appBaseURL;
    private final BookingService bookingService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public String createCheckoutSession(UUID bookingId, String userEmail) throws StripeException {

        Booking booking = bookingService.findByIdAndUser(bookingId, userEmail);

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Booking is not pending status");
        }

        BigDecimal totalAmount = booking.getTotalPrice();
        long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        String successUrl = appBaseURL + "/payment/success?bookingId=" + bookingId;
        String cancelUrl = appBaseURL + "/payment/cancel?bookingId=" + bookingId;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("mxn")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Room Reservation: " + booking.getRoom().getName())
                                                                .setDescription("Dates: " + booking.getStartDate() + " to " + booking.getEndDate())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("bookingId", booking.getId().toString())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public void confirmBookingPayment(UUID bookingId) {
        Booking booking = bookingService.findBookingById(bookingId);

        if (booking == null) {
            log.error("Booking not found for ID: {}", bookingId);
            System.err.println("Webhook received for non-existent booking: " + bookingId);
            return;
        }

        if (booking.getBookingStatus() == BookingStatus.PENDING) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);

        } else {
            System.out.println("Booking " + bookingId + " is already in status " + booking.getBookingStatus() + ", no change needed.");
        }
    }
}
