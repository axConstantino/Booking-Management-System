package com.axconstantino.reservationsystem.payment.controller;

import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {
    private String webhookSecret;
    private final PaymentService paymentService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Received Stripe event: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (StripeException e) {
            log.error("Stripe webhook error.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Stripe error");
        }

        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if (dataObject == null) {
            log.warn("Webhook event data object is null. Type: {}", event.getType());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data object");
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                log.info("Handling checkout.session.completed event");
                Session session = (Session) dataObject;
                handleCheckoutSessionCompleted(session);
                break;

            default:
                log.warn("Unhandled event type: {}", event.getType());
                break;
        }

        return ResponseEntity.ok("Success");
    }

    private void handleCheckoutSessionCompleted(Session session) {
        if ("paid".equals(session.getPaymentStatus())) {
            String bookingIdStr = session.getMetadata().get("booking_id");
            if (bookingIdStr != null) {
                try {
                    UUID bookingId = UUID.fromString(bookingIdStr);
                    log.info("Confirming payment for booking ID: {}", bookingId);
                    paymentService.confirmBookingPayment(bookingId);
                    log.info("Booking {} status updated to CONFIRMED.", bookingId);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid UUID format in booking_id metadata: {}", bookingIdStr, e);
                    // Podrías querer notificar a un administrador sobre este error
                } catch (Exception e) {
                    log.error("Error confirming booking payment for ID: {}", bookingIdStr, e);
                    // Implementar lógica de reintento o notificación aquí si es necesario
                }
            } else {
                log.error("Booking ID metadata not found in checkout session: {}", session.getId());
                // Esto es un error grave en tu lógica de creación de sesión
            }
        } else {
            log.warn("Checkout session {} completed but payment status is {}. Not confirming booking.", session.getId(), session.getPaymentStatus());
            // Podrías querer manejar otros estados si es relevante (ej: 'unpaid' aunque raro para completed)
        }
    }

}
