package dev.getelements.elements.stripe.event;

import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.StripeEvents;

import java.util.List;
import java.util.Map;

/**
 * Published when Stripe fires {@code checkout.session.completed}.
 *
 * <p>The {@code sessionId} matches the {@code stripeReference} stored on pending purchase rows,
 * so consumers can reconcile without any additional DAO lookups. {@code paymentIntentId} is set
 * for {@code payment} mode sessions; {@code subscriptionId} is set for {@code subscription} mode.
 * {@code metadata} carries whatever was stamped on the Checkout Session (e.g. {@code orgId},
 * {@code addonId}), removing the need for customer→org or price→addon lookups.
 */
public record StripeCheckoutSessionCompletedEvent(
        String sessionId,
        String customerId,
        String paymentIntentId,
        String subscriptionId,
        String mode,
        Map<String, String> metadata) implements Event {

    @Override
    public String getEventName() {
        return StripeEvents.CHECKOUT_SESSION_COMPLETED;
    }

    @Override
    public List<Object> getEventArguments() {
        return List.of(sessionId, customerId, mode);
    }

}
