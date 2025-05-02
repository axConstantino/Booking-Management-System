package com.axconstantino.reservationsystem.payment.service;

import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Service responsible for handling incoming Stripe webhook events.
 * <p>
 *     Events are dispatched based on their type and processed appropriately:
 *     <ul>
 *         <li>checkout.session.completed -> confirms a booking payment.</li>
 *         <li>payment_intent.succeeded -> confirms a booking payment.</li>
 *         <li>payment_intent.payment_failed -> marks booking as payment failed.</li>
 *         <li>charge.refunded -> marks booking as refunded</li>
 *         <li>Any other event type will be logged as unhandled.</li>
 *     </ul>
 * </p>
 * <p>
 *     The {@link #handleEventAsync(Event)} method runs
 *     using the “stripeAsyncExecutor” thread pool to avoid blocking the main thread.
 * </p>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookHandlerService {

    /**
     * Service delegating actual booking state changes
     * (confirm, fail, refund) based on webhook outcome.
     */
    private final PaymentService paymentService;

    /**
     * Asynchronously handles a received Stripe {@link Event}.
     * <p>
     *     Logs the event ID and type, delegates to {@link #handleEvent(Event)},
     *     and catches any exceptions to ensure they are logged without crashing.
     * </p>
     *
     * @param event the Stripe event payload
     */
    @Async("stripeAsyncExecutor")
    public void handleEventAsync(Event event) {
        log.info("Processing async event ID: {}, Type: {}", event.getId(), event.getType());
        try {
            handleEvent(event);
            log.info("Event {} processed successfully", event.getId());
        } catch (Exception e) {
            log.error("Critical error processing event {}: {}", event.getId(), e.getMessage(), e);
        }
    }

    /**
     * Synchronously routes the Stripe {@link Event} to the correct handler
     * method based on its type. If the event data object cannot be deserialized,
     * throws an IllegalStateException.
     *
     * @param event Stripe event to handle.
     * @throws IllegalStateException if the event data object is null.
     */
    public void handleEvent(Event event) {
        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if (dataObject == null) {
            log.error("Null data object in event ID: {}", event.getId());
            throw new IllegalStateException("Invalid data in Stripe event");
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted((Session) dataObject);
                    break;

                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded((PaymentIntent) dataObject);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed((PaymentIntent) dataObject);
                    break;

                case "charge.refunded":
                    handleChargeRefunded((Charge) dataObject);
                    break;

                default:
                    log.warn("Unhandled event type: {}", event.getType());
                    break;
            }
        } catch (ClassCastException e) {
            log.error("Type error in event {}: {}", event.getType(), e.getMessage());
            throw new IllegalStateException("Incorrect object type for event: " + event.getType());
        }
    }

    /**
     * Handles a completed Checkout Session by validating its "paid" status
     * and confirmed the corresponding booking .
     *
     * @param session the completes Stripe Checkout Session.
     */
    private void handleCheckoutSessionCompleted(Session session) {
        validateAndProcess(
                session,
                session.getPaymentStatus(),
                "paid",
                paymentService::confirmBookingPayment
        );
    }

    /**
     * Handles a succeeded PaymentIntent by validating its “succeeded” status
     * and confirming the corresponding booking.
     *
     * @param paymentIntent the succeeded Stripe PaymentIntent
     */
    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        validateAndProcess(
                paymentIntent,
                paymentIntent.getStatus(),
                "succeeded",
                paymentService::confirmBookingPayment
        );
    }

    /**
     * Handles a failed PaymentIntent by marking the corresponding booking
     * status as PAYMENT_FAILED.
     *
     * @param paymentIntent the failed Stripe PaymentIntent
     */
    private void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        processBookingId(
                paymentIntent.getMetadata(),
                paymentService::markBookingAsPaymentFailed,
                "PaymentIntent"
        );
    }

    /**
     * Handles a refunded Charge by marking the corresponding booking
     * status as REFUNDED.
     *
     * @param charge the Stripe Charge that was refunded
     */
    private void handleChargeRefunded(Charge charge) {
        processBookingId(
                charge.getMetadata(),
                paymentService::refundBooking,
                "Charge"
        );
    }

    /**
     * Parses a bookingId UUID from a Stripe session’s metadata.
     *
     * @param session the Stripe Session containing metadata
     * @return the parsed booking UUID
     * @throws IllegalArgumentException if the UUID format is invalid
     */
    private UUID extractBookingId(Session session) {
        return parseBookingId(session.getMetadata().get("bookingId"));
    }

    /**
     * Parses a bookingId UUID from a PaymentIntent’s metadata.
     *
     * @param paymentIntent the PaymentIntent containing metadata
     * @return the parsed booking UUID
     * @throws IllegalArgumentException if the UUID format is invalid
     */
    private UUID extractBookingId(PaymentIntent paymentIntent) {
        return parseBookingId(paymentIntent.getMetadata().get("bookingId"));
    }

    /**
     * Parses a bookingId UUID from a Charge’s metadata.
     *
     * @param charge the Charge containing metadata
     * @return the parsed booking UUID
     * @throws IllegalArgumentException if the UUID format is invalid
     */
    private UUID extractBookingId(Charge charge) {
        return parseBookingId(charge.getMetadata().get("bookingId"));
    }

    /**
     * Converts a string to a UUID, logging and rethrowing on failure.
     *
     * @param bookingIdStr the string representation of the booking UUID
     * @return the parsed UUID
     * @throws IllegalArgumentException if the string is not a valid UUID
     */
    private UUID parseBookingId(String bookingIdStr) {
        try {
            return UUID.fromString(bookingIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid booking ID: {}", bookingIdStr);
            throw new IllegalArgumentException("Invalid UUID format for bookingId");
        }
    }

    /**
     * Validates that the Stripe Session’s payment status matches the expected value,
     * then processes the bookingId via the provided action.
     *
     * @param session        the Stripe Session to validate
     * @param currentStatus  the session’s actual payment status
     * @param expectedStatus the status required to proceed (e.g. "paid")
     * @param action         Consumer that takes a bookingId UUID to perform an action
     */
    private void validateAndProcess(Session session,
                                    String currentStatus,
                                    String expectedStatus,
                                    Consumer<UUID> action) {
        validateAndProcessGeneric(
                session.getMetadata(),
                currentStatus,
                expectedStatus,
                action,
                "Session"
        );
    }

    /**
     * Validates that the PaymentIntent’s status matches the expected value,
     * then processes the bookingId via the provided action.
     *
     * @param paymentIntent  the Stripe PaymentIntent to validate
     * @param currentStatus  the intent’s actual status
     * @param expectedStatus the status required to proceed (e.g. "succeeded")
     * @param action         Consumer that takes a bookingId UUID to perform an action
     */
    private void validateAndProcess(PaymentIntent paymentIntent,
                                    String currentStatus,
                                    String expectedStatus,
                                    Consumer<UUID> action) {
        validateAndProcessGeneric(
                paymentIntent.getMetadata(),
                currentStatus,
                expectedStatus,
                action,
                "PaymentIntent"
        );
    }

    /**
     * Generic status validation and bookingId processing logic.
     * Logs a warning if the current status does not match expected.
     *
     * @param metadata       metadata map containing the bookingId
     * @param currentStatus  actual status from Stripe object
     * @param expectedStatus status required to proceed
     * @param action         Consumer to apply when validation succeeds
     * @param context        textual context for logging (e.g. "Session", "Charge")
     */
    private void validateAndProcessGeneric(Map<String, String> metadata,
                                           String currentStatus,
                                           String expectedStatus,
                                           Consumer<UUID> action,
                                           String context) {
        if (!expectedStatus.equals(currentStatus)) {
            log.warn("Invalid status in {}: {} (expected: {})", context, currentStatus, expectedStatus);
            return;
        }

        processBookingId(metadata, action, context);
    }

    /**
     * Extracts the bookingId from the given metadata and applies the processor.
     * Logs errors for missing or invalid IDs, and rethrows unexpected exceptions.
     *
     * @param metadata  metadata map containing the bookingId key
     * @param processor Consumer to execute with the parsed bookingId
     * @param context   textual context for logging (e.g. "PaymentIntent", "Charge")
     */
    private void processBookingId(Map<String, String> metadata, Consumer<UUID> processor, String context) {
        String bookingIdStr = metadata.get("bookingId");
        if (bookingIdStr == null) {
            log.error("Booking ID not found in {}", context);
            return;
        }

        try {
            UUID bookingId = parseBookingId(bookingIdStr);
            processor.accept(bookingId);
        } catch (IllegalArgumentException e) {
            log.error("Error processing booking ID in {}: {}", context, e.getMessage());
        } catch (Exception e) {
            log.error("Critical error in {}: {}", context, e.getMessage(), e);
            throw e;
        }
    }
}