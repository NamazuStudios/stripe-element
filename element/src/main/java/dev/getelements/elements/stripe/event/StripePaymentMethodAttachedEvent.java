package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

public record StripePaymentMethodAttachedEvent(
        String paymentMethodId,
        String customerId) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.PAYMENT_METHOD_ATTACHED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(paymentMethodId, customerId);
    }

}
