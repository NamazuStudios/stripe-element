package dev.getelements.elements.stripe.service;

/**
 * Thrown by {@link StripeService#recordMeterEvent} when Stripe has no active meter configured for
 * the given event name. Distinguishes this specific, actionable condition — create a meter in the
 * Stripe dashboard with the exact event name, or point the SKU's catalogue entry at an existing
 * meter — from any other Stripe API failure, so callers don't have to pattern-match Stripe's error
 * message text themselves.
 */
public class NoSuchMeterException extends RuntimeException {

    private final String eventName;

    public NoSuchMeterException(String eventName, String message) {
        super(message);
        this.eventName = eventName;
    }

    /** The meter event name Stripe rejected as having no active meter configured. */
    public String getEventName() {
        return eventName;
    }

}