package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecord;
import dev.getelements.elements.stripe.model.PostingRecordResponse;
import dev.getelements.elements.stripe.model.StripeMode;

/**
 * The posting-record store: a shared, queryable ledger of what each billing meter-event posting
 * attempt actually did, so billing verification never depends on pod logs or manual Stripe
 * dashboard hunts.
 *
 * <p><strong>Division of responsibility:</strong> postings that flow through
 * {@link StripeService#recordMeterEvent} — POSTED and FAILED — are recorded automatically by
 * this element, including the Stripe mode and the Stripe-side {@code identifier}. Consumer
 * elements only call {@link #recordPostingOutcome} for the decisions that never reach that
 * method: skips (exempt org, no customer attached yet) and any post-hoc corrections. This keeps
 * the caller's boilerplate to a minimum while the ledger still captures the full picture.
 *
 * <p>This ledger is the identifier-level record of what <em>we</em> decided to post. Stripe's
 * API exposes no listing of raw meter events, so Stripe-side confirmation is done at the
 * aggregate level via {@link StripeService#listMeterUsage}: sum the ledger's POSTED quantities
 * for a (customer, meter, window) and compare against Stripe's aggregated value.
 */
public interface MeterPostingService {

    /**
     * Records one posting decision. Consumer elements call this at decision points that do not
     * flow through {@link StripeService#recordMeterEvent} (which records POSTED/FAILED itself) —
     * i.e. skips and post-hoc corrections. The store stamps {@code at} if the record leaves it
     * unset. Append-only — retries of the same identifier record their own entry, which is the
     * point: the ledger shows the history.
     *
     * @param record the decision to persist
     */
    void recordPostingOutcome(PostingRecord record);

    /**
     * Lists posting records, newest first. All filters are optional ({@code null} = match all);
     * {@code mode = null} means both modes (the admin's "sandbox / production / both" filter).
     *
     * @param identifierPrefix matches records whose identifier starts with this (the idempotency
     *                        key convention {@code "batchId:orgId:meterName"} makes batch- and
     *                        org-scoped queries natural), or {@code null} for all
     * @param customerId       exact Stripe customer ID match, or {@code null} for all
     * @param eventName        exact meter event name match, or {@code null} for all
     * @param outcome          exact outcome match, or {@code null} for all
     * @param mode             restrict to one Stripe mode, or {@code null} for both
     * @param limit            page size
     * @param offset           page offset
     */
    PostingRecordResponse listPostings(
            String identifierPrefix,
            String customerId,
            String eventName,
            PostingOutcome outcome,
            StripeMode mode,
            int limit,
            int offset);

}
