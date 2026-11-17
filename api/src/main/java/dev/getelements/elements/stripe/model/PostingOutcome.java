package dev.getelements.elements.stripe.model;

/**
 * What happened when a consumer element attempted to post a billing meter event. Deliberately
 * does <em>not</em> encode sandbox/production in the outcome (unlike {@code POSTED_LIVE} /
 * {@code POSTED_TEST} phrasing): every attempt targets exactly one {@link StripeMode}, so the
 * mode is stored as its own field and the sandbox/production/both filter applies uniformly to
 * posted, skipped, and failed records alike.
 */
public enum PostingOutcome {

    /** The meter event was accepted by Stripe in the record's mode. */
    POSTED,

    /** Skipped without posting: the org is exempt (e.g. trial) and nothing should be billed. */
    SKIPPED_EXEMPT,

    /** Skipped without posting: no Stripe customer has been attached yet (backfill later). */
    SKIPPED_NO_CUSTOMER,

    /** The attempt failed after retries; {@code detail} carries the failure reason. */
    FAILED,

    /** A previously pending/failed attempt was successfully recovered. */
    RECOVERED_STALE_PENDING

}
