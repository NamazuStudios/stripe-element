package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.StripeEventLogResponse;

public interface StripeEventLogService {

    /**
     * Persists a record of a received, verified Stripe webhook event.
     * Called by the webhook endpoint after signature validation.
     */
    void logEvent(String stripeEventId, String eventType);

    /**
     * Lists webhook events, newest first.
     *
     * @param type   Stripe event type filter (e.g. {@code "payment_intent.succeeded"}); {@code null} for all types
     * @param limit  maximum results per page
     * @param offset zero-based offset for pagination
     */
    StripeEventLogResponse listEvents(String type, int limit, int offset);

}
