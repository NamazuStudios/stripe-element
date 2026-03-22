package dev.getelements.elements.stripe;

/**
 * Event name constants published by the Stripe Element.
 * Use these with {@code @ElementEventConsumer} to subscribe to Stripe webhook events.
 *
 * <pre>{@code
 * @ElementEventConsumer(StripeEvents.PAYMENT_SUCCEEDED)
 * public void onPaymentSucceeded() { ... }
 * }</pre>
 */
public final class StripeEvents {

    public static final String PAYMENT_SUCCEEDED  = "payment_intent.succeeded";
    public static final String PAYMENT_FAILED     = "payment_intent.payment_failed";
    public static final String SUBSCRIPTION_CREATED   = "customer.subscription.created";
    public static final String SUBSCRIPTION_CANCELLED = "customer.subscription.deleted";

    private StripeEvents() {}

}
