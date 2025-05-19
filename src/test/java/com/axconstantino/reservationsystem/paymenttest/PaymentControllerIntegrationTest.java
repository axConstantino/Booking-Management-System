package com.axconstantino.reservationsystem.paymenttest;


import com.axconstantino.reservationsystem.payment.dto.CheckoutSessionResponse;
import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.stripe.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private final UUID bookingId = UUID.randomUUID();

    @Test
    @WithMockUser(username = "user@example.com")
    void initiatePayment_ValidRequest_ReturnsUrl() throws Exception {
        CheckoutSessionResponse response = new CheckoutSessionResponse("https://stripe.com/pay", "session_id");
        given(paymentService.createCheckoutSession(eq(bookingId), anyString()))
                .willReturn(response);

        mockMvc.perform(post("/payments/" + bookingId + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://stripe.com/pay"))
                .andExpect(jsonPath("$.sessionId").value("session_id"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void initiatePayment_StripeException_Returns500() throws Exception {
        given(paymentService.createCheckoutSession(eq(bookingId), anyString()))
                .willThrow(new ApiException("Stripe error", null, null, null, null));

        mockMvc.perform(post("/payments/" + bookingId + "/pay"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.url").value("Error initiating payment: Stripe error"));
    }

    @Test
    void paymentSuccess_ReturnsMessage() throws Exception {
        mockMvc.perform(get("/payments/success").param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payment successful")));
    }

    @Test
    void paymentCancel_ReturnsMessage() throws Exception {
        mockMvc.perform(get("/payments/cancel").param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payment canceled")));
    }
}