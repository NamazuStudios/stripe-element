package dev.getelements.elements.stripe.model;

/**
 * Current status of a Stripe subscription.
 *
 * @param subscriptionId   Stripe subscription ID ({@code sub_...})
 * @param status            Stripe subscription status (e.g. {@code "active"}, {@code "canceled"})
 * @param currentPeriodEnd  ISO-8601 timestamp when the current billing period ends
 */
public record SubscriptionStatusResponse(String subscriptionId, String status, String currentPeriodEnd) {}
