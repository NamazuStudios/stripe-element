package dev.getelements.elements.stripe.model;

/**
 * Response from creating a Stripe PaymentIntent for a one-off purchase.
 *
 * @param paymentIntentId Stripe PaymentIntent ID ({@code pi_...})
 * @param clientSecret    secret passed to Stripe.js ({@code stripe.confirmCardPayment}) to complete the charge
 */
public record CreatePaymentIntentResponse(String paymentIntentId, String clientSecret) {}
