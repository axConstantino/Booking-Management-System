package com.axconstantino.reservationsystem.payment.controller;

import com.axconstantino.reservationsystem.payment.service.StripeWebhookHandlerService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final StripeWebhookHandlerService stripeWebhookService;
    private final String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Received Stripe event: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        try {
            stripeWebhookService.handleEvent(event);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Error processing Stripe event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Event processing failed");
        }
    }
}