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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service responsible for handling all Stripe-based payment operations
 * related to Booking entities, as well as updating booking statuses
 * according to payment outcomes.
 *
 * <p> This service:</p>
 * <ul>
 *     <li>Initializes Stripe API credentials on startup.</li>
 *     <li>Creates secure Checkout Sessions for clients to pay for a booking.</li>
 *     <li>Processes payment confirmations, failures, ans refunds.</li>
 * </ul>
 * <p>All public methods run within a transaccional context to ensure
 * data consistency when updating booking statuses.</p>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    /**
     * Stripe secret API key, injected from application properties
     * at property path {@code app.stripe.api-key}.
     */
    @Value("${app.stripe.api-key}")
    private String stripeSecretKey;

    /**
     * Base URL of the application, injected from application properties
     * at property path {@code app.base-url}.
     */
    @Value("${app.base-url}")
    private String appBaseURL;

    /**
     * Service for retrieving and updating Booking entities
     * form database.
     */
    private final BookingService bookingService;

    /**
     * After bean construction, configures the Stripe client
     * with the injected secret key.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Creates a Stripe Checkout Session for the given booking.
     *
     * <p>
     *     Validates that the booking exists, belongs to the specified user,
     *     and is still in PENDING status. Then constructs
     *     a line item with the room name, reservation dates, and total amount.
     *     The session's success and cancellation URLs include the bookingId
     *     as a query parameter.
     * </p>
     * @param bookingId the UUID of the booking to be paid.
     * @param userEmail the email address of the booking owner.
     * @return a URL string pointing to the Stripe Checkout page.
     * @throws ConflictException if the booking status is not PENDING.
     * @throws StripeException if any error occurs during the Stripe API call.
     */
    @Transactional
    public String createCheckoutSession(UUID bookingId, String userEmail) throws StripeException {

        //1. Retrieve and validate booking
        Booking booking = bookingService.findByIdAndUser(bookingId, userEmail);

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Booking is not pending status");
        }

        //2. Convert total price to cents (Stripe expects amounts in the smallest currency unit)
        BigDecimal totalAmount = booking.getTotalPrice();
        long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        //3. Build success and cancel URLs
        String successUrl = appBaseURL + "/payment/success?bookingId=" + bookingId;
        String cancelUrl = appBaseURL + "/payment/cancel?bookingId=" + bookingId;

        //4. Assemble Stripe session parameters
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

        //5. Create and return the session URL
        Session session = Session.create(params);
        return session.getUrl();
    }

    /**
     * Marks the specified booking as CONFIRMED if its current status is PENDING.
     *<p>
     *     Intended to be called when a payment_intent.succeeded webhook
     *     is received from Stripe.
     *</p>
     * @param bookingId the UUID of the booking to confirm
     */
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
            log.info("Booking {} is in status {}, no change needed.", bookingId, booking.getBookingStatus());
        }
    }

    /**
     * Marks the specified booking as PAYMENT_FAILED if its current status is PENDING.
     * <p>
     *     Intended to be called when a payment_intent.payment_failed webhook
     *     is received from Stripe.
     * </p>
     *
     * @param bookingId the UUID of the booking to mark as failed.
     */
    @Transactional
    public void markBookingAsPaymentFailed(UUID bookingId) {
        Booking booking = bookingService.findBookingById(bookingId);

        if (booking == null) {
            log.error("Booking not found for ID: {}", bookingId);
            return;
        }

        if (booking.getBookingStatus() == BookingStatus.PENDING) {
            booking.setBookingStatus(BookingStatus.PAYMENT_FAILED);
            log.info("Booking {} status updated to PAYMENT_FAILED.", bookingId);
        } else {
            log.info("Booking {} is in status {}, no change needed.", bookingId, booking.getBookingStatus());
        }
    }

    /**
     * Sets the specified booking status to REFUNDED if it is currently CONFIRMED.
     *<p>
     *     Intended for use when issuing a refund via the Stripe API and then
     *     reflecting that change locally.
     *</p>
     *
     * @param bookingId the UUID of the booking to refund.
     */
    @Transactional
    public void refundBooking(UUID bookingId) {
        Booking booking = bookingService.findBookingById(bookingId);

        if (booking == null) {
            log.error("Booking not found for ID: {}", bookingId);
            return;
        }

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            booking.setBookingStatus(BookingStatus.REFUNDED);
            log.info("Booking {} status updated to REFUNDED.", bookingId);
        } else {
            log.info("Booking {} is in status {}, no change needed.", bookingId, booking.getBookingStatus());
        }
    }
}
