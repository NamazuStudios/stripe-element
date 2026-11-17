package dev.getelements.elements.stripe.model;

import java.util.List;

/**
 * A page of Stripe customers from this account's customer list.
 *
 * @param customers  customers in this page
 * @param hasMore    {@code true} if another page is available via {@code nextCursor}
 * @param nextCursor customer ID cursor to pass as {@code startingAfter} for the next page,
 *                   or {@code null} if this is the last page
 */
public record CustomerListResponse(
        List<CustomerSummary> customers,
        boolean hasMore,
        String nextCursor) {}
