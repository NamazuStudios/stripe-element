package dev.getelements.elements.stripe.model;

import java.util.Map;

/**
 * Request to create a Stripe-hosted Checkout Session.
 *
 * @param customerId     Stripe customer ID ({@code cus_...})
 * @param priceId        Stripe price ID ({@code price_...}) to add as a line item
 * @param successUrl     URL Stripe redirects to on successful payment/setup
 * @param cancelUrl      URL Stripe redirects to if the customer cancels
 * @param mode           {@code "subscription"} (default if null) or {@code "payment"} for one-off charges
 * @param idempotencyKey optional caller-supplied idempotency key; retrying the same key returns the
 *                       original session without creating a second one
 * @param metadata       optional key-value pairs stamped on both the Checkout Session and the
 *                       resulting Subscription or PaymentIntent; useful for carrying {@code orgId},
 *                       {@code addonId}, etc. so webhook handlers can skip DAO lookups
 */
public record CreateCheckoutSessionRequest(
        String customerId,
        String priceId,
        String successUrl,
        String cancelUrl,
        String mode,
        String idempotencyKey,
        Map<String, String> metadata) {}
