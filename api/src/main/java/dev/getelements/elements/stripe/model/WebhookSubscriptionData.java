package dev.getelements.elements.stripe.model;

/**
 * Typed representation of the subscription data carried inside a Stripe webhook event.
 * Used in tests to build payloads without raw JSON strings, and can be reused anywhere
 * a structured view of webhook subscription data is needed.
 *
 * @param id         the Stripe Subscription id ({@code sub_...})
 * @param customerId the Stripe Customer id ({@code cus_...})
 * @param status     Stripe subscription status (e.g. {@code "active"}, {@code "canceled"})
 */
public record WebhookSubscriptionData(
        String id,
        String customerId,
        String status
) {}
