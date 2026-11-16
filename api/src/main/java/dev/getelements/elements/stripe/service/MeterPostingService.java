package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecord;
import dev.getelements.elements.stripe.model.PostingRecordResponse;
import dev.getelements.elements.stripe.model.StripeMode;

/**
 * The posting-record store: a shared, queryable ledger of what each billing meter-event posting
 * attempt actually did. Consumer elements call {@link #recordPostingOutcome} at each
 * reconciliation decision point (posted / skipped-exempt / skipped-no-customer / failed /
 * recovered), through this exported service, so every billing-capable element feeds the same
 * store and an admin can verify billing from one place.
 *
 * <p>This ledger is the identifier-level record of what <em>we</em> decided to post. Stripe's
 * API exposes no listing of raw meter events, so Stripe-side confirmation is done at the
 * aggregate level via {@link StripeService#listMeterUsage}: sum the ledger's POSTED quantities
 * for a (customer, meter, window) and compare against Stripe's aggregated value.
 */
public interface MeterPostingService {

    /**
     * Records one posting decision. Called by consumer elements at the moment the decision is
     * made; the store stamps {@code at} if the record leaves it unset. Append-only — retries
     * of the same identifier record their own entry (e.g. {@code FAILED} followed by
     * {@code RECOVERED_STALE_PENDING}), which is the point: the ledger shows the history.
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
