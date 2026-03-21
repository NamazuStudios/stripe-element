package dev.getelements.elements.stripe.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.param.PaymentIntentCreateParams;

public interface StripeGateway {

    PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException;

    Subscription retrieveSubscription(String subscriptionId) throws StripeException;

}
