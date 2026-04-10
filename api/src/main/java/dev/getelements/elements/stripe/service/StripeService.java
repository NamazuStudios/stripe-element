package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;

public interface StripeService {

    String METADATA_USER_ID = "userId";

    CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request);

    SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId);

    /**
     * Records a receipt for a confirmed payment. Called from the webhook handler after
     * Stripe fires {@code payment_intent.succeeded} or {@code invoice.payment_succeeded}.
     * Silently skips if {@code userId} is null or blank.
     */
    void recordPaymentReceipt(String transactionId, long amount, String currency, String userId);

}
