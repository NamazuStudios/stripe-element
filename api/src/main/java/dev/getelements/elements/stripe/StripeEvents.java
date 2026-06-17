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

    public static final String PAYMENT_SUCCEEDED           = "payment_intent.succeeded";
    public static final String PAYMENT_FAILED              = "payment_intent.payment_failed";
    public static final String INVOICE_PAYMENT_SUCCEEDED   = "invoice.payment_succeeded";
    public static final String INVOICE_PAYMENT_FAILED      = "invoice.payment_failed";
    public static final String SUBSCRIPTION_CREATED        = "customer.subscription.created";
    public static final String SUBSCRIPTION_UPDATED        = "customer.subscription.updated";
    public static final String SUBSCRIPTION_CANCELLED      = "customer.subscription.deleted";
    public static final String SUBSCRIPTION_TRIAL_WILL_END = "customer.subscription.trial_will_end";

    /** Published when a Stripe payment_intent.canceled webhook is received. */
    public static final String PAYMENT_CANCELED          = "payment_intent.canceled";

    /** Published when a Stripe setup_intent.succeeded webhook is received. */
    public static final String SETUP_INTENT_SUCCEEDED    = "setup_intent.succeeded";

    /** Published when a Stripe payment_method.attached webhook is received. */
    public static final String PAYMENT_METHOD_ATTACHED   = "payment_method.attached";

    /** Published when a Stripe checkout.session.completed webhook is received. */
    public static final String CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";

    /** Published for every verified Stripe webhook regardless of type. */
    public static final String RAW_WEBHOOK = "stripe.webhook";

    private StripeEvents() {}

}
