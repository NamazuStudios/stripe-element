package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;

import java.util.List;

public record StripePaymentFailedEvent(String paymentIntentId, String failureMessage) implements Event {

    public static final String NAME = "payment_intent.payment_failed";

    @Override
    public String getEventName() {
        return NAME;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(paymentIntentId, failureMessage);
    }

}
