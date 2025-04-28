package com.axconstantino.reservationsystem.payment.service;

import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookHandlerService {

    private final PaymentService paymentService;

    public void handleEvent(Event event) {
        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if (dataObject == null) {
            log.error("Stripe event with null data object. Event type: {}", event.getType());
            throw new IllegalStateException("Received event with null data object. Type: " + event.getType());
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted((Session) dataObject);
                break;

            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded((Session) dataObject);
                break;

            case "payment_intent.payment_failed":
                handlePaymentIntentFailed((Session) dataObject);
                break;

            case "charge.refunded":
                handleChargeRefunded((Charge) dataObject);
                break;

            default:
                log.warn("Unhandled Stripe event type received: {}", event.getType());
                break;
        }
    }


    private void handleCheckoutSessionCompleted(Session session) {
        if (!"paid".equals(session.getPaymentStatus())) {
            log.warn("Checkout session {} completed but payment status is {}. Ignoring.", session.getId(), session.getPaymentStatus());
            return;
        }

        String bookingIdStr = session.getMetadata().get("bookingId");
        if (bookingIdStr == null) {
            log.error("No bookingId metadata in checkout.session.completed event for session {}", session.getId());
            return;
        }

        try {
            UUID bookingId = UUID.fromString(bookingIdStr);
            log.info("Confirming booking payment for booking ID: {}", bookingId);
            paymentService.confirmBookingPayment(bookingId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for bookingId: {}", bookingIdStr, e);
        } catch (Exception e) {
            log.error("Error confirming booking payment for bookingId: {}", bookingIdStr, e);
        }
    }

    private void handlePaymentIntentSucceeded(Session session) {
        String bookingIdStr = session.getMetadata().get("bookingId");
        if (bookingIdStr == null) {
            log.error("No bookingId metadata in payment_intent.succeeded event for session {}", session.getId());
            return;
        }

        try {
            UUID bookingId = UUID.fromString(bookingIdStr);
            log.info("Async payment succeeded for booking ID: {}", bookingId);
            paymentService.confirmBookingPayment(bookingId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for bookingId: {}", bookingIdStr, e);
        } catch (Exception e) {
            log.error("Error confirming async payment for bookingId: {}", bookingIdStr, e);
        }
    }

    private void handlePaymentIntentFailed(Session session) {
        String bookingIdStr = session.getMetadata().get("bookingId");
        if (bookingIdStr == null) {
            log.error("No bookingId metadata in async_payment_failed event for session {}", session.getId());
            return;
        }

        try {
            UUID bookingId = UUID.fromString(bookingIdStr);
            log.warn("Async payment failed for booking ID: {}", bookingId);
            paymentService.markBookingAsPaymentFailed(bookingId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for bookingId: {}", bookingIdStr, e);
        } catch (Exception e) {
            log.error("Error marking async payment failed for bookingId: {}", bookingIdStr, e);
        }
    }

    private void handleChargeRefunded(Charge charge) {
        String bookingIdStr = charge.getMetadata().get("bookingId");
        if (bookingIdStr == null) {
            log.error("No bookingId metadata in charge.refunded event for charge {}", charge.getId());
            return;
        }

        try {
            UUID bookingId = UUID.fromString(bookingIdStr);
            log.info("Charge refunded, processing refund for booking ID: {}", bookingId);
            paymentService.refundBooking(bookingId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for bookingId: {}", bookingIdStr, e);
        } catch (Exception e) {
            log.error("Error processing refund for bookingId: {}", bookingIdStr, e);
        }
    }
}
