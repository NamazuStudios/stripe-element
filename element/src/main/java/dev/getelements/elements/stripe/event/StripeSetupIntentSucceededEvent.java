package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

public record StripeSetupIntentSucceededEvent(
        String setupIntentId,
        String customerId) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.SETUP_INTENT_SUCCEEDED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(setupIntentId, customerId);
    }

}
