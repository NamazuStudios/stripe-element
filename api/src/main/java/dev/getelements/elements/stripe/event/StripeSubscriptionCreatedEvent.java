package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;

import java.util.List;

public record StripeSubscriptionCreatedEvent(String subscriptionId, String customerId, String status) implements Event {

    public static final String NAME = "customer.subscription.created";

    @Override
    public String getEventName() {
        return NAME;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(subscriptionId, customerId, status);
    }

}
