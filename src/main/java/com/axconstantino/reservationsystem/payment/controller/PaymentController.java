package com.axconstantino.reservationsystem.payment.controller;

import com.axconstantino.reservationsystem.payment.dto.PaymentInitiateResponse;
import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Endpoints for initiating and handling payment flows via Stripe")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(
            summary = "Initiate payment for a booking",
            description = "Creates a Stripe Checkout Session for the given booking ID and logged-in user. Returns a URL to redirect the client to Stripe.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Checkout session created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PaymentInitiateResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error creating Stripe checkout session",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PaymentInitiateResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Booking not found or does not belong to user"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Booking is not in PENDING status"
                    )
            }
    )
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

    @Operation(
            summary = "Handle successful payment",
            description = "Endpoint called by Stripe redirect upon successful payment. Confirms booking and returns a friendly message.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment successful message returned"),
                    @ApiResponse(responseCode = "400", description = "Invalid bookingId parameter")
            }
    )
    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(
            @Parameter(
                    description = "UUID of the booking just paid",
                    required = true,
                    schema = @Schema(format = "uuid")
            )
            @RequestParam UUID bookingId) {
        return ResponseEntity.ok("!Payment successful! Your reservation is confirmed. ID: " + bookingId);
    }
    @Operation(
            summary = "Handle canceled payment",
            description = "Endpoint called by Stripe redirect when the user cancels payment. Returns a cancellation message.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment canceled message returned"),
                    @ApiResponse(responseCode = "400", description = "Invalid bookingId parameter")
            }
    )
    @GetMapping("/cancel")
    public ResponseEntity<String> paymentCancel(
            @Parameter(
                    description = "UUID of the booking whose payment was canceled",
                    required = true,
                    schema = @Schema(format = "uuid")
            )
            @RequestParam UUID bookingId
    ) {
        return ResponseEntity.ok(
                "Payment canceled for reservation ID: " + bookingId
        );
    }
}
