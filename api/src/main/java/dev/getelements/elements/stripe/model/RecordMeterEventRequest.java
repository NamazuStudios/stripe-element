package dev.getelements.elements.stripe.model;

import java.math.BigDecimal;

/**
 * Request body for recording a Stripe billing meter event.
 *
 * @param customerId     Stripe customer ID ({@code cus_...})
 * @param eventName      name of the meter as configured in the Stripe Dashboard
 * @param value          usage quantity to report, e.g. {@code 0.25} (must be &gt; 0)
 * @param idempotencyKey caller-supplied key used by Stripe to deduplicate events;
 *                       re-submitting the same key will not create a duplicate charge
 */
public record RecordMeterEventRequest(
        String customerId,
        String eventName,
        BigDecimal value,
        String idempotencyKey) {

    /**
     * Convenience constructor for callers reporting whole-unit usage.
     */
    public RecordMeterEventRequest(String customerId, String eventName, long value, String idempotencyKey) {
        this(customerId, eventName, BigDecimal.valueOf(value), idempotencyKey);
    }

}
