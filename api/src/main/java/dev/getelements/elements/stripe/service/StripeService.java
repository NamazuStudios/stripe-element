package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;

public interface StripeService {

    CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request);

    SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId);

}
