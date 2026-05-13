package dev.getelements.elements.stripe.model;

import java.util.List;

public record SubscriptionListResponse(
        List<SubscriptionStatusResponse> subscriptions,
        boolean hasMore,
        String nextCursor) {}
