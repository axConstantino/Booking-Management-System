package com.axconstantino.reservationsystem.paymenttest;

import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.axconstantino.reservationsystem.payment.service.StripeWebhookHandlerService;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StripeWebhookHandlerServiceUnitTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private StripeWebhookHandlerService stripeWebhookHandler;

    private Event event;
    private EventDataObjectDeserializer deserializer;

    @BeforeEach
    void setUp() {
        event = mock(Event.class);
        deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    }

    @Test
    void handleCheckoutSessionCompleted_ValidPaidStatus_CallsConfirmBooking() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", UUID.randomUUID().toString());
        when(session.getMetadata()).thenReturn(metadata);

        when(deserializer.getObject()).thenReturn(Optional.of(session));
        when(event.getType()).thenReturn("checkout.session.completed");
        stripeWebhookHandler.handleEvent(event);

        verify(paymentService).confirmBookingPayment(any(UUID.class));
    }

    @Test
    void handlePaymentIntentFailed_ValidMetadata_CallsMarkAsFailed() {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", UUID.randomUUID().toString());
        when(paymentIntent.getMetadata()).thenReturn(metadata);

        when(deserializer.getObject()).thenReturn(Optional.of(paymentIntent));
        when(event.getType()).thenReturn("payment_intent.payment_failed");

        stripeWebhookHandler.handleEvent(event);

        verify(paymentService).markBookingAsPaymentFailed(any(UUID.class));
    }

    @Test
    void handleEvent_InvalidBookingId_LogsError() {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", "invalid-uuid");
        when(session.getMetadata()).thenReturn(metadata);

        when(deserializer.getObject()).thenReturn(Optional.of(session));
        when(event.getType()).thenReturn("checkout.session.completed");

        stripeWebhookHandler.handleEvent(event);

        verify(paymentService, never()).confirmBookingPayment(any());
    }

    @Test
    void handleEvent_UnhandledType_LogsWarning() {
        when(deserializer.getObject()).thenReturn(Optional.of(mock(Charge.class)));
        when(event.getType()).thenReturn("unknown.event.type");

        stripeWebhookHandler.handleEvent(event);

        verifyNoInteractions(paymentService);
    }

    @Test
    void handleEvent_DataObjectNull_ThrowsException() {
        when(deserializer.getObject()).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> {
            stripeWebhookHandler.handleEvent(event);
        });
    }

    @Test
    void handlePaymentIntentSucceeded_InvalidStatus_NoAction() {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getStatus()).thenReturn("requires_payment_method");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", UUID.randomUUID().toString());
        when(paymentIntent.getMetadata()).thenReturn(metadata);

        when(deserializer.getObject()).thenReturn(Optional.of(paymentIntent));
        when(event.getType()).thenReturn("payment_intent.succeeded");

        stripeWebhookHandler.handleEvent(event);

        verify(paymentService, never()).confirmBookingPayment(any());
    }

    @Test
    void handleEvent_ClassCastException_ThrowsIllegalState() {
        when(deserializer.getObject()).thenReturn(Optional.of(mock(Charge.class)));
        when(event.getType()).thenReturn("payment_intent.succeeded");

        assertThrows(IllegalStateException.class, () -> stripeWebhookHandler.handleEvent(event));
    }
}