package dev.getelements.elements.stripe.service;

import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;

@ElementServiceExport
public interface StripeService {

    CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request);

    SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId);

}
