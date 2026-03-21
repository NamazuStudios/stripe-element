package dev.getelements.elements.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.param.PaymentIntentCreateParams;
import dev.getelements.elements.stripe.service.StripeGateway;

/**
 * StripeGateway implementation for integration tests. Delegates directly to the
 * static Stripe SDK methods, relying on Stripe.apiKey already being set by the test.
 */
class LiveStripeGateway implements StripeGateway {

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException {
        return PaymentIntent.create(params);
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

}
