package dev.getelements.elements.stripe.model;

import java.util.Map;

/**
 * @param amount                   amount in the currency's smallest unit (e.g. cents)
 * @param currency                 ISO 4217 lowercase currency code
 * @param customerId               Stripe customer ID ({@code cus_...}); may be null for guest checkouts
 * @param description              optional description shown on the Stripe dashboard and invoice
 * @param metadata                 optional key-value pairs stored on the PaymentIntent for reconciliation
 * @param automaticPaymentMethods  if {@code true}, enables automatic payment methods so PaymentElement
 *                                 renders all enabled methods, not just card
 * @param setupFutureUsage         set to {@code "off_session"} to save the payment method for later
 *                                 charges without re-prompting the customer
 * @param idempotencyKey           optional caller-supplied idempotency key; retrying the same key is
 *                                 a no-op — Stripe returns the original result without double-charging
 */
public record CreatePaymentIntentRequest(
        long amount,
        String currency,
        String customerId,
        String description,
        Map<String, String> metadata,
        Boolean automaticPaymentMethods,
        String setupFutureUsage,
        String idempotencyKey) {

    /** Convenience factory for the common case where only amount/currency/customer are needed. */
    public static CreatePaymentIntentRequest of(long amount, String currency, String customerId) {
        return new CreatePaymentIntentRequest(amount, currency, customerId, null, null, null, null, null);
    }

}
