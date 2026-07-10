package dev.getelements.elements.stripe.model;

import java.util.List;

/**
 * A page of subscriptions for a Stripe customer.
 *
 * @param subscriptions subscriptions in this page, newest first
 * @param hasMore       {@code true} if another page is available via {@code nextCursor}
 * @param nextCursor    subscription ID cursor to pass as {@code startingAfter} for the next page,
 *                      or {@code null} if this is the last page
 */
public record SubscriptionListResponse(
        List<SubscriptionStatusResponse> subscriptions,
        boolean hasMore,
        String nextCursor) {}
