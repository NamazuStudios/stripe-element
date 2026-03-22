package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.param.PaymentIntentCreateParams;
import dev.getelements.elements.stripe.StripeApplication;
import jakarta.inject.Named;

public class DefaultStripeGateway implements StripeGateway {

    @Inject
    DefaultStripeGateway(@Named(StripeApplication.STRIPE_API_KEY) String apiKey) {
        Stripe.apiKey = apiKey;
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException {
        return PaymentIntent.create(params);
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

}
