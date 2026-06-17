package dev.getelements.elements.stripe.model;

/**
 * Request body for recording a Stripe billing meter event.
 *
 * @param customerId     Stripe customer ID ({@code cus_...})
 * @param eventName      name of the meter as configured in the Stripe Dashboard
 * @param value          usage quantity to report (must be &gt; 0)
 * @param idempotencyKey caller-supplied key used by Stripe to deduplicate events;
 *                       re-submitting the same key will not create a duplicate charge
 */
public record RecordMeterEventRequest(
        String customerId,
        String eventName,
        long value,
        String idempotencyKey) {}
