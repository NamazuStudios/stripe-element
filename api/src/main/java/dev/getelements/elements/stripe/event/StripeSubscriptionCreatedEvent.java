package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;


import java.util.List;

public record StripeSubscriptionCreatedEvent(
        String subscriptionId,
        String customerId,
        String status,
        String orgId) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.SUBSCRIPTION_CREATED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(subscriptionId, customerId, status);
    }

}