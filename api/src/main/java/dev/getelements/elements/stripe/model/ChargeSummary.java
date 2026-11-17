package dev.getelements.elements.stripe.model;

/**
 * A charge against a Stripe customer — the ground truth of money movement for one-time
 * purchases (PaymentIntents and Checkout {@code payment}-mode sessions produce charges;
 * charges back subscriptions and metered invoices too).
 *
 * @param id              Stripe charge ID ({@code ch_...})
 * @param paymentIntentId owning PaymentIntent ({@code pi_...}), or {@code null} for
 *                        non-intent charges
 * @param amount          amount in the smallest currency unit
 * @param refundedAmount  amount refunded so far, in the smallest currency unit
 * @param currency        ISO 4217 currency code
 * @param status          Stripe charge status ({@code succeeded}, {@code failed}, ...)
 * @param description     charge description, or {@code null}
 * @param receiptUrl      Stripe-hosted receipt URL, or {@code null}
 * @param refunded        whether the charge is fully refunded
 * @param created         creation time, ISO-8601
 */
public record ChargeSummary(
        String id,
        String paymentIntentId,
        long amount,
        long refundedAmount,
        String currency,
        String status,
        String description,
        String receiptUrl,
        boolean refunded,
        String created) {}
