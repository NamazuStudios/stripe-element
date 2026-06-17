package dev.getelements.elements.stripe.model;

/**
 * Summarises a Stripe Price for display or product-selection purposes.
 *
 * @param id         Stripe price ID ({@code price_...})
 * @param productId  Stripe product ID this price belongs to ({@code prod_...})
 * @param nickname   optional human-readable label set in the Stripe Dashboard; may be {@code null}
 * @param unitAmount price in the smallest currency unit (e.g. cents); {@code null} for usage-based prices
 * @param currency   ISO 4217 currency code (e.g. {@code "usd"})
 * @param type       {@code "one_time"} or {@code "recurring"}
 * @param interval   billing interval for recurring prices: {@code "day"}, {@code "week"},
 *                   {@code "month"}, or {@code "year"}; {@code null} for one-time prices
 */
public record PriceSummary(
        String id,
        String productId,
        String nickname,
        Long unitAmount,
        String currency,
        String type,
        String interval) {}
