package com.axconstantino.reservationsystem.payment.controller;

import com.axconstantino.reservationsystem.payment.database.ProcessedEvent;
import com.axconstantino.reservationsystem.payment.database.repository.EventRepository;
import com.axconstantino.reservationsystem.payment.service.StripeWebhookHandlerService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Tag(name = "Stripe Webhook", description = "Handling incoming events from Stripe")
public class StripeWebhookController {

    private final StripeWebhookHandlerService stripeWebhookService;
    private final EventRepository eventRepository;

    @Value("${app.payments.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${app.debug:false}")
    private boolean debugMode;

    @Operation(
            summary = "Process Stripe events.",
            description = "Receives signed events from Stripe, validates their authenticity, and processes them.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Event processed successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid webhook signature"),
                    @ApiResponse(responseCode = "409", description = "Event already processed"),
                    @ApiResponse(responseCode = "500", description = "Internal error processing event")
            }
    )
    @Transactional
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Received Stripe event: {} (ID: {})", event.getType(), event.getId());

            if (eventRepository.existsById(event.getId())) {
                log.warn("Duplicate event detected: {}", event.getId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Event already processed");
            }

        } catch (SignatureVerificationException e) {
            log.error("Invalid signature for event: {}", e.getSigHeader(), e);
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Error building event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing event");
        }

        try {
            eventRepository.save(new ProcessedEvent(event.getId()));

            stripeWebhookService.handleEventAsync(event);

            return ResponseEntity.ok("Event processing started");

        } catch (Exception e) {
            log.error("Error processing event {}: {}", event.getId(), e.getMessage(), e);
            eventRepository.deleteById(event.getId());

            String errorMessage = debugMode
                    ? "Error: " + e.getMessage() + " | Event ID: " + event.getId()
                    : "Event processing failed";

            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }
}