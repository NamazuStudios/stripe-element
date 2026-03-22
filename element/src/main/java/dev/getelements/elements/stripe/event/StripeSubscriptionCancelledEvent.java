package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

public record StripeSubscriptionCancelledEvent(String subscriptionId, String customerId) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.SUBSCRIPTION_CANCELLED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(subscriptionId, customerId);
    }

}
