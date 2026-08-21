package dev.getelements.elements.stripe.model;

/**
 * Distinguishes which Stripe account a request should be routed to. A deployment may have both a
 * {@link #PRODUCTION} (live-mode) and a {@link #SANDBOX} (test-mode) Stripe account configured at
 * the same time; callers select which one to use on a per-request basis.
 */
public enum StripeMode {

    PRODUCTION,

    SANDBOX

}
