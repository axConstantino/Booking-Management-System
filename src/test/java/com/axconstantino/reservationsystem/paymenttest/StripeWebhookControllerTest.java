package com.axconstantino.reservationsystem.paymenttest;

import com.axconstantino.reservationsystem.payment.controller.StripeWebhookController;
import com.axconstantino.reservationsystem.payment.database.repository.EventRepository;
import com.axconstantino.reservationsystem.payment.service.StripeWebhookHandlerService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class StripeWebhookControllerTest {

    @Mock
    private StripeWebhookHandlerService stripeWebhookService;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private StripeWebhookController controller;

    private final String webhookSecret = "test_secret";
    private final String payload = "{}";
    private final String validSig = "v1_valid";
    private final String invalidSig = "v1_invalid";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(controller, "webhookSecret", webhookSecret);
        ReflectionTestUtils.setField(controller, "debugMode", false);

        reset(eventRepository, stripeWebhookService);
    }

    @Test
    void handleStripeWebhook_ValidSignatureAndNewEvent_ReturnsOk() {
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getId()).thenReturn("evt_123");
            when(mockEvent.getType()).thenReturn("checkout.session.completed");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, validSig, webhookSecret))
                    .thenReturn(mockEvent);

            when(eventRepository.existsById("evt_123")).thenReturn(false);

            ResponseEntity<String> response = controller.handleStripeWebhook(payload, validSig);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(eventRepository).save(argThat(event -> event.getId().equals("evt_123")));
            verify(stripeWebhookService).handleEventAsync(mockEvent);
        }
    }

    @Test
    void handleStripeWebhook_InvalidSignature_ReturnsBadRequest() {
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(payload, invalidSig, webhookSecret))
                    .thenThrow(new SignatureVerificationException("Invalid Signature", invalidSig));

            ResponseEntity<String> response = controller.handleStripeWebhook(payload, invalidSig);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(eventRepository, stripeWebhookService);
        }
    }

    @Test
    void handleStripeWebhook_DuplicateEvent_ReturnsConflict() {
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getId()).thenReturn("evt_123");
            when(mockEvent.getType()).thenReturn("checkout.session.completed");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, validSig, webhookSecret))
                    .thenReturn(mockEvent);

            when(eventRepository.existsById("evt_123")).thenReturn(true);

            ResponseEntity<String> response = controller.handleStripeWebhook(payload, validSig);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            verify(eventRepository, never()).save(any());
            verifyNoInteractions(stripeWebhookService);
        }
    }

    @Test
    void handleStripeWebhook_ErrorSavingEvent_DeletesEventAndReturnsError() {
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getId()).thenReturn("evt_123");
            when(mockEvent.getType()).thenReturn("checkout.session.completed");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, validSig, webhookSecret))
                    .thenReturn(mockEvent);

            when(eventRepository.existsById("evt_123")).thenReturn(false);
            doThrow(new RuntimeException("Error in Database")).when(eventRepository).save(any());

            ResponseEntity<String> response = controller.handleStripeWebhook(payload, validSig);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            verify(eventRepository).deleteById("evt_123");
        }
    }

    @Test
    void handleStripeWebhook_DebugModeEnabled_ReturnsDetailedErrorMessage() {
        ReflectionTestUtils.setField(controller, "debugMode", true);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getId()).thenReturn("evt_123");
            when(mockEvent.getType()).thenReturn("checkout.session.completed");

            mockedWebhook.when(() -> Webhook.constructEvent(payload, validSig, webhookSecret))
                    .thenReturn(mockEvent);

            when(eventRepository.existsById("evt_123")).thenReturn(false);
            doThrow(new RuntimeException("Critical Error")).when(eventRepository).save(any());

            ResponseEntity<String> response = controller.handleStripeWebhook(payload, validSig);

            assertTrue(response.getBody().contains("Critical Error"));
            assertTrue(response.getBody().contains("evt_123"));
        }
    }
}
