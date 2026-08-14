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
 * @param price       the recurring Price billing this meter's usage (i.e. the one whose
 *                     {@code recurring.meter} points back at this meter's id), or {@code null} if
 *                     no such Price exists yet. When more than one Price references the same meter,
 *                     an arbitrary one is returned — configure exactly one per meter in Stripe.
 */
public record MeterSummary(
        String id,
        String displayName,
        String eventName,
        String status,
        PriceSummary price) {}
