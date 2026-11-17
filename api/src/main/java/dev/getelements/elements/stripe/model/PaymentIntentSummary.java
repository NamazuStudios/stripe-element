package dev.getelements.elements.stripe.model;

/**
 * A PaymentIntent — the intent-level view of one-time purchases. Includes attempts that never
 * produced a successful charge (failed, requires payment method, uncaptured), which is what
 * makes it useful for debugging checkout problems rather than confirming revenue.
 *
 * @param id          Stripe PaymentIntent ID ({@code pi_...})
 * @param amount      amount in the smallest currency unit
 * @param currency    ISO 4217 currency code
 * @param status      Stripe PaymentIntent status ({@code succeeded}, {@code requires_payment_method},
 *                    {@code canceled}, ...)
 * @param description intent description, or {@code null}
 * @param created     creation time, ISO-8601
 */
public record PaymentIntentSummary(
        String id,
        long amount,
        String currency,
        String status,
        String description,
        String created) {}
