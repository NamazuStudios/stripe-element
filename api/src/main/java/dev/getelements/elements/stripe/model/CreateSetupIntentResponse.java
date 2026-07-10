package dev.getelements.elements.stripe.model;

/**
 * Response from creating a Stripe SetupIntent used to collect a payment method without an immediate charge.
 *
 * @param setupIntentId Stripe SetupIntent ID ({@code seti_...})
 * @param clientSecret  secret passed to Stripe.js ({@code stripe.confirmCardSetup}) to attach the payment method
 */
public record CreateSetupIntentResponse(String setupIntentId, String clientSecret) {}