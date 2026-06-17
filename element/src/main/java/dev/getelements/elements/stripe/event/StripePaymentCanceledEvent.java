package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.Arrays;
import java.util.List;

public record StripePaymentCanceledEvent(
        String paymentIntentId,
        String customerId) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.PAYMENT_CANCELED;
    }

    @Override
    public List<Object> getEventArguments() {
        return Arrays.asList(paymentIntentId, customerId);
    }

}
