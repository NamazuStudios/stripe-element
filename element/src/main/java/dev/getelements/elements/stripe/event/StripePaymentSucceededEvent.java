package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

public record StripePaymentSucceededEvent(
        String paymentIntentId,
        long amount,
        String currency,
        String customerId) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.PAYMENT_SUCCEEDED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(paymentIntentId, amount, currency);
    }

}
