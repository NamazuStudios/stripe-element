package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

public record StripePaymentFailedEvent(String paymentIntentId, String failureMessage) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.PAYMENT_FAILED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(paymentIntentId, failureMessage);
    }

}
