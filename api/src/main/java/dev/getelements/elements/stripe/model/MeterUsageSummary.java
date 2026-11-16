package dev.getelements.elements.stripe.model;

import java.math.BigDecimal;

/**
 * An aggregated usage total for one billing meter and one customer over one window of time, as
 * reported by Stripe's {@code GET /v1/billing/meters/{id}/event_summaries}.
 *
 * <p>This is the read-side counterpart to {@link dev.getelements.elements.stripe.service.StripeService#recordMeterEvent}:
 * Stripe's API deliberately exposes no listing of <em>raw</em> meter events (the meter-event
 * object is write-only in the v1 REST API), so usage verification is done against these
 * aggregates — e.g. comparing a total recorded by a consumer element's posting ledger against
 * {@link #aggregatedValue()} for the same customer, meter, and window.
 */
public record MeterUsageSummary(
        String id,
        String meterId,
        String eventName,
        BigDecimal aggregatedValue,
        String startTime,
        String endTime) {}
