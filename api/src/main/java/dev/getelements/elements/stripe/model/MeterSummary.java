package dev.getelements.elements.stripe.model;

/**
 * Summarises a Stripe Billing Meter for display or metered-price-configuration purposes.
 *
 * @param id          Stripe meter ID ({@code mtr_...})
 * @param displayName human-readable name set in the Stripe Dashboard
 * @param eventName   the event name usage is recorded against ({@code meter_event_name} passed to
 *                     {@code createMeterEvent}) — this is the value catalogue configuration should
 *                     store to associate a billable unit with this meter
 * @param status      {@code "active"} or {@code "inactive"}
 */
public record MeterSummary(
        String id,
        String displayName,
        String eventName,
        String status) {}
