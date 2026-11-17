package dev.getelements.elements.stripe.model;

import java.util.List;

/**
 * A cached rollup of per-customer metered usage over a trailing window — the account-wide
 * answer Stripe's API does not provide natively (event summaries require a per-customer query,
 * so this is computed by fanning out and cached briefly).
 *
 * @param totals     one entry per customer on the account
 * @param windowDays the trailing window the totals cover, in days
 * @param computedAt when the rollup was computed, ISO-8601 (not when cached)
 */
public record CustomerUsageRollup(
        List<CustomerUsageTotal> totals,
        int windowDays,
        String computedAt) {}
