package com.axconstantino.reservationsystem.payment.controller;

import com.axconstantino.reservationsystem.payment.dto.PaymentInitiateResponse;
import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("{bookingId}/pay")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();

        try {
            String checkoutUrl = paymentService.createCheckoutSession(bookingId, userEmail);
            return ResponseEntity.ok(new PaymentInitiateResponse(checkoutUrl));
        } catch (StripeException e) {
            log.info("Error creating Stripe checkout session: {}", e.getMessage());
            return ResponseEntity.status(500).body(new PaymentInitiateResponse("Error initiating payment " + e.getMessage()));
        }
    }

    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(@RequestParam UUID bookingId) {
        // Handle successful payment logic here
        return ResponseEntity.ok("!Payment successful! Your reservation is confirmed. ID: " + bookingId);
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> paymentCancel(@RequestParam UUID bookingId) {
        // Handle canceled payment logic here
        return ResponseEntity.ok("Payment canceled for reservation ID: " + bookingId);
    }
}
