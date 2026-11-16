package dev.getelements.elements.stripe.model;

import java.util.Map;

/**
 * A Stripe customer as discovered from this account's own customer list — the basis of the
 * billing-posture view ("which customers are billing-enabled, and what landed for them?").
 * Metadata is included verbatim: consumer elements that store org identity on the customer
 * (e.g. {@code orgId}/{@code orgName}) get human-readable rows for free.
 *
 * @param id       Stripe customer ID ({@code cus_...})
 * @param name     display name, or {@code null} if unset
 * @param email    email address, or {@code null} if unset
 * @param metadata the customer's metadata map (never {@code null}, may be empty)
 * @param created  creation time, ISO-8601
 */
public record CustomerSummary(
        String id,
        String name,
        String email,
        Map<String, String> metadata,
        String created) {}
