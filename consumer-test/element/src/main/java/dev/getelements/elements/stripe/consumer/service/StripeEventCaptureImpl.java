package dev.getelements.elements.stripe.consumer.service;

import dev.getelements.elements.sdk.annotation.ElementEventConsumer;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Receives events published by the Stripe Element and accumulates their primary IDs.
 *
 * <p>Method parameter types match the positional arguments returned by each event's
 * {@code getEventArguments()} — see the corresponding event records in the Stripe Element.
 */
public class StripeEventCaptureImpl implements StripeEventCapture {

    private final List<String> paymentSucceededIds = Collections.synchronizedList(new ArrayList<>());

    private final List<String> paymentFailedIds = Collections.synchronizedList(new ArrayList<>());

    private final List<String> subscriptionCreatedIds = Collections.synchronizedList(new ArrayList<>());

    private final List<String> subscriptionCancelledIds = Collections.synchronizedList(new ArrayList<>());

    @ElementEventConsumer(StripeEvents.PAYMENT_SUCCEEDED)
    public void onPaymentSucceeded(String paymentIntentId, Long amount, String currency) {
        paymentSucceededIds.add(paymentIntentId);
    }

    @ElementEventConsumer(StripeEvents.PAYMENT_FAILED)
    public void onPaymentFailed(String paymentIntentId, String failureMessage) {
        paymentFailedIds.add(paymentIntentId);
    }

    @ElementEventConsumer(StripeEvents.SUBSCRIPTION_CREATED)
    public void onSubscriptionCreated(String subscriptionId, String customerId, String status) {
        subscriptionCreatedIds.add(subscriptionId);
    }

    @ElementEventConsumer(StripeEvents.SUBSCRIPTION_CANCELLED)
    public void onSubscriptionCancelled(String subscriptionId, String customerId) {
        subscriptionCancelledIds.add(subscriptionId);
    }

    @Override
    public List<String> paymentSucceededIds() {
        return List.copyOf(paymentSucceededIds);
    }

    @Override
    public List<String> paymentFailedIds() {
        return List.copyOf(paymentFailedIds);
    }

    @Override
    public List<String> subscriptionCreatedIds() {
        return List.copyOf(subscriptionCreatedIds);
    }

    @Override
    public List<String> subscriptionCancelledIds() {
        return List.copyOf(subscriptionCancelledIds);
    }

    @Override
    public void clear() {
        paymentSucceededIds.clear();
        paymentFailedIds.clear();
        subscriptionCreatedIds.clear();
        subscriptionCancelledIds.clear();
    }

}
