package com.axconstantino.reservationsystem.paymenttest;

import com.axconstantino.reservationsystem.payment.service.PaymentService;
import com.axconstantino.reservationsystem.payment.service.StripeWebhookHandlerService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest
@EnableAsync
@ActiveProfiles("test")
public class StripeWebhookHandlerServiceIntegrationTest {

    @Autowired
    private StripeWebhookHandlerService stripeWebhookHandler;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void handleEventAsync_ValidEvent_ProcessesSuccessfully() throws InterruptedException {
        Event event = createTestEvent("checkout.session.completed", new HashMap<>() {{
            put("bookingId", UUID.randomUUID().toString());
        }});

        stripeWebhookHandler.handleEventAsync(event);

        Thread.sleep(1000);
        verify(paymentService).confirmBookingPayment(any(UUID.class));
    }


    private Event createTestEvent(String type, Map<String, String> metadata) {
        Session session = new Session();
        session.setPaymentStatus("paid");
        session.setMetadata(metadata);

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        Event event = mock(Event.class);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }
}