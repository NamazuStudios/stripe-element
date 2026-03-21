package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;

import java.util.List;

public record StripePaymentSucceededEvent(String paymentIntentId, long amount, String currency) implements Event {

    public static final String NAME = "payment_intent.succeeded";

    @Override
    public String getEventName() {
        return NAME;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(paymentIntentId, amount, currency);
    }

}
