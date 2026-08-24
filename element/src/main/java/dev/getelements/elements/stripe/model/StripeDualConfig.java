package dev.getelements.elements.stripe.model;

/**
 * Wire format for {@code /stripe/config}: both the production and sandbox credential sets in a
 * single request/response body.
 */
public record StripeDualConfig(StripeConfig production, StripeConfig sandbox) {}
