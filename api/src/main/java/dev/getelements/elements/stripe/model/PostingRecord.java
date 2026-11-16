package dev.getelements.elements.stripe.model;

import java.math.BigDecimal;

/**
 * A single billing-posting decision, recorded by a consumer element at the moment its
 * reconciler decides what to do with one (batch, org, meter) group. Feeds the posting-record
 * store via {@link dev.getelements.elements.stripe.service.MeterPostingService#recordPostingOutcome}
 * so billing verification never depends on pod logs or manual Stripe dashboard hunts.
 *
 * <p>For {@link PostingOutcome#POSTED} records, {@link #identifier()} is the same idempotency
 * key passed to {@code StripeService.recordMeterEvent} — it is the join key between this ledger
 * and the Stripe-side event.
 *
 * @param identifier  idempotency key of the posting attempt, e.g. {@code "batchId:orgId:meterName"}
 * @param customerId  Stripe customer ID the event targets, or {@code null} when skipped for
 *                    want of a customer
 * @param eventName   meter event name (as passed to {@code recordMeterEvent})
 * @param quantity    usage quantity for the group
 * @param outcome     what the reconciler decided (see {@link PostingOutcome})
 * @param mode        the Stripe mode this attempt targets — production or sandbox
 * @param detail      human-readable explanation (skip reason, failure detail); may be {@code null}
 * @param at          epoch millis of the decision; the store stamps it when the record omits it
 */
public record PostingRecord(
        String identifier,
        String customerId,
        String eventName,
        BigDecimal quantity,
        PostingOutcome outcome,
        StripeMode mode,
        String detail,
        Long at) {}
