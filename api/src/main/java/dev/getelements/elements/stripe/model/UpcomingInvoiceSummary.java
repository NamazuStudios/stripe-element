package dev.getelements.elements.stripe.model;

import java.util.List;

/**
 * A preview of the customer's next invoice, as returned by Stripe's upcoming-invoice endpoint.
 * This is where metered usage accumulates into money: pending meter events appear here as a
 * usage line item for the current billing period. Absent when the customer has nothing
 * pending — no subscription and no usage attached to a metered price.
 *
 * @param total           invoice total in the smallest currency unit (negative means credit)
 * @param currency        ISO 4217 currency code
 * @param periodStart     billing period start, ISO-8601
 * @param periodEnd       billing period end (the charge date), ISO-8601
 * @param lines           the invoice's line items
 */
public record UpcomingInvoiceSummary(
        long total,
        String currency,
        String periodStart,
        String periodEnd,
        List<UpcomingInvoiceLine> lines) {

    /**
     * One line item on the upcoming invoice. Usage-based (metered) lines carry the aggregated
     * quantity for the period in {@code quantity}.
     *
     * @param description line description (e.g. "1 × API Requests (at $0.01 / request)")
     * @param amount      line amount in the smallest currency unit
     * @param quantity    billed quantity, where applicable (metered lines: aggregated usage)
     * @param type        Stripe line type ({@code subscription}, {@code invoiceitem}, {@code usage}, ...)
     */
    public record UpcomingInvoiceLine(
            String description,
            long amount,
            Long quantity,
            String type) {}
}
