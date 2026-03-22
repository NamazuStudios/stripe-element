package dev.getelements.elements.stripe;

import dev.getelements.elements.stripe.event.StripePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCancelledEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCreatedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StripeEventTest {

    @Test
    void paymentSucceeded_name() {
        final var event = new StripePaymentSucceededEvent("pi_001", 1000L, "usd");
        assertEquals(StripeEvents.PAYMENT_SUCCEEDED, event.getEventName());
        assertFalse(event.isSystemEvent());
    }

    @Test
    void paymentSucceeded_arguments() {
        final var event = new StripePaymentSucceededEvent("pi_001", 1000L, "usd");
        assertEquals(List.of("pi_001", 1000L, "usd"), event.getEventArguments());
    }

    @Test
    void paymentFailed_name() {
        final var event = new StripePaymentFailedEvent("pi_002", "card declined");
        assertEquals(StripeEvents.PAYMENT_FAILED, event.getEventName());
        assertFalse(event.isSystemEvent());
    }

    @Test
    void paymentFailed_arguments() {
        final var event = new StripePaymentFailedEvent("pi_002", "card declined");
        assertEquals(List.of("pi_002", "card declined"), event.getEventArguments());
    }

    @Test
    void subscriptionCreated_name() {
        final var event = new StripeSubscriptionCreatedEvent("sub_001", "cus_001", "active");
        assertEquals(StripeEvents.SUBSCRIPTION_CREATED, event.getEventName());
        assertFalse(event.isSystemEvent());
    }

    @Test
    void subscriptionCreated_arguments() {
        final var event = new StripeSubscriptionCreatedEvent("sub_001", "cus_001", "active");
        assertEquals(List.of("sub_001", "cus_001", "active"), event.getEventArguments());
    }

    @Test
    void subscriptionCancelled_name() {
        final var event = new StripeSubscriptionCancelledEvent("sub_002", "cus_002");
        assertEquals(StripeEvents.SUBSCRIPTION_CANCELLED, event.getEventName());
        assertFalse(event.isSystemEvent());
    }

    @Test
    void subscriptionCancelled_arguments() {
        final var event = new StripeSubscriptionCancelledEvent("sub_002", "cus_002");
        assertEquals(List.of("sub_002", "cus_002"), event.getEventArguments());
    }

}
