package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.param.PaymentIntentCreateParams;
import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.StripeApplication;

public class DefaultStripeGateway implements StripeGateway {

    @Inject
    DefaultStripeGateway() {

        final var element = ElementSupplier.getElementLocal(DefaultStripeGateway.class).get();

        Stripe.apiKey = (String) element.getElementRecord().attributes()
                .getAttribute(StripeApplication.STRIPE_API_KEY);
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
