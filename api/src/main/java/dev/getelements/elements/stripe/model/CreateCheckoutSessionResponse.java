package dev.getelements.elements.stripe.model;

/**
 * Response from creating a Stripe-hosted Checkout Session.
 *
 * @param sessionId Stripe Checkout Session ID ({@code cs_...})
 * @param url       single-use hosted checkout URL to redirect the customer to
 */
public record CreateCheckoutSessionResponse(String sessionId, String url) {}
