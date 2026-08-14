package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

/**
 * Published for every verified Stripe webhook, regardless of type.
 * Use this to handle event types that do not have a dedicated typed event.
 */
public record StripeRawEvent(String type, String eventId, String rawJson) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.RAW_WEBHOOK;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(type, eventId, rawJson);
    }

}
