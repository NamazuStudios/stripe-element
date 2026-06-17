package dev.getelements.elements.stripe.model;

import java.util.Map;

/**
 * Request body for creating a Stripe subscription.
 *
 * @param priceId        Stripe price ID ({@code price_...}); the customer must already have a default
 *                       payment method on file (attached via {@code createSetupIntent})
 * @param description    optional description stored on the subscription for dashboard reconciliation
 * @param metadata       optional key-value pairs stored on the subscription (e.g. {@code org_id},
 *                       {@code addon_id}) for reconciliation in the Stripe dashboard
 * @param idempotencyKey optional caller-supplied idempotency key; retrying the same key is a no-op —
 *                       Stripe returns the original result without creating a duplicate subscription
 */
public record CreateSubscriptionRequest(
        String priceId,
        String description,
        Map<String, String> metadata,
        String idempotencyKey) {

    /** Convenience factory for the common case where only a price ID is needed. */
    public static CreateSubscriptionRequest of(String priceId) {
        return new CreateSubscriptionRequest(priceId, null, null, null);
    }

}
