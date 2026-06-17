package dev.getelements.elements.stripe.model;

/**
 * Summary of a Stripe invoice.
 *
 * @param id             Stripe invoice ID ({@code in_...})
 * @param subscriptionId the subscription this invoice belongs to, or {@code null} for one-off invoices
 * @param amountPaid     amount collected in the currency's smallest unit (e.g. cents)
 * @param currency       ISO 4217 lowercase currency code
 * @param status         Stripe invoice status: {@code draft}, {@code open}, {@code paid},
 *                       {@code uncollectible}, or {@code void}
 * @param createdAt      ISO-8601 timestamp of invoice creation
 */
public record InvoiceSummary(
        String id,
        String subscriptionId,
        Long amountPaid,
        String currency,
        String status,
        String createdAt) {}
