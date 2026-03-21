package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;

import java.util.List;

public record StripeSubscriptionCancelledEvent(String subscriptionId, String customerId) implements Event {

    public static final String NAME = "customer.subscription.deleted";

    @Override
    public String getEventName() {
        return NAME;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(subscriptionId, customerId);
    }

}
