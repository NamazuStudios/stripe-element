package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

public record StripeInvoicePaymentSucceededEvent(
        String invoiceId,
        String paymentIntentId,
        long amountPaid,
        String currency) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.INVOICE_PAYMENT_SUCCEEDED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(invoiceId, paymentIntentId, amountPaid, currency);
    }

}
