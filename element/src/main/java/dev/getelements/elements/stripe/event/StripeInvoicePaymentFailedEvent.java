package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;

public record StripeInvoicePaymentFailedEvent(
        String invoiceId,
        String subscriptionId,
        String customerId,
        String failureMessage) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.INVOICE_PAYMENT_FAILED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(invoiceId, subscriptionId, customerId, failureMessage);
    }

}
