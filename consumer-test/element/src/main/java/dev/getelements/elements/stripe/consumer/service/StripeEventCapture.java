package dev.getelements.elements.stripe.consumer.service;

import java.util.List;

/**
 * Collects event IDs published by the Stripe Element for inspection in integration tests.
 * Exposed via {@link StripeEventCaptureImpl} and queryable through the REST endpoint.
 */
public interface StripeEventCapture {

    List<String> paymentSucceededIds();

    List<String> paymentFailedIds();

    List<String> subscriptionCreatedIds();

    List<String> subscriptionCancelledIds();

    void clear();

}
