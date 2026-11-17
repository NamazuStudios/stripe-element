package dev.getelements.elements.stripe.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Computed usage totals for one Stripe customer over a trailing window.
 *
 * @param customerId  Stripe customer ID ({@code cus_...})
 * @param total       summed usage across all active meters for the window, or {@code null} if
 *                    any meter's aggregate could not be fetched (unknown, not zero)
 * @param byMeter     summed usage per meter event name; meters that could not be fetched are
 *                    omitted
 */
public record CustomerUsageTotal(
        String customerId,
        BigDecimal total,
        Map<String, BigDecimal> byMeter) {}
