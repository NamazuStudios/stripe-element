package dev.getelements.elements.stripe.model;

/**
 * Typed representation of the payment intent data carried inside a Stripe webhook event.
 * Used in tests to build payloads without raw JSON strings, and can be reused anywhere
 * a structured view of webhook payment-intent data is needed.
 *
 * @param id             the Stripe PaymentIntent id ({@code pi_...})
 * @param amount         amount in the currency's smallest unit (e.g. cents)
 * @param currency       ISO 4217 lowercase currency code (e.g. {@code "usd"})
 * @param status         Stripe status string (e.g. {@code "succeeded"}, {@code "requires_payment_method"})
 * @param failureMessage error message from {@code last_payment_error}; {@code null} for succeeded events
 */
public record WebhookPaymentIntentData(
        String id,
        long amount,
        String currency,
        String status,
        String failureMessage
) {}
