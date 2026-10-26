package dev.getelements.elements.stripe.model;

/**
 * Stripe credential configuration stored in the database.
 *
 * <p>When submitted via PUT the full values are expected; masking is a client-side UI concern only.
 *
 * @param apiKey         Stripe secret API key ({@code sk_live_...} or {@code sk_test_...})
 * @param webhookSecret  Stripe webhook signing secret ({@code whsec_...})
 */
public record StripeConfig(String apiKey, String webhookSecret) {}
