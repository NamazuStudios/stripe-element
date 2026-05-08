package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.CreateCustomerResponse;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreateSetupIntentResponse;
import dev.getelements.elements.stripe.model.PaymentMethodSummary;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;

import java.util.List;

public interface StripeService {

    String METADATA_USER_ID = "userId";

    /** Metadata key used to associate a Stripe customer/subscription with an org. */
    String METADATA_ORG_ID = "orgId";

    /**
     * Creates a Stripe Customer. The {@code orgId} is stored in the customer's metadata under
     * {@link #METADATA_ORG_ID} so that subsequent webhook events can be reverse-looked-up to the org.
     */
    CreateCustomerResponse createCustomer(String email, String name, String orgId);

    /**
     * Creates a SetupIntent for the given customer. Returns the client secret that the frontend
     * passes to Stripe.js to collect and attach a payment method without charging upfront.
     */
    CreateSetupIntentResponse createSetupIntent(String customerId);

    /**
     * Lists the payment methods attached to a customer. Useful for checking whether a customer
     * has a card on file before attempting a charge.
     */
    List<PaymentMethodSummary> listPaymentMethods(String customerId);

    CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request);

    SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId);

    /**
     * Records a receipt for a confirmed payment. Called from the webhook handler after
     * Stripe fires {@code payment_intent.succeeded} or {@code invoice.payment_succeeded}.
     * Silently skips if {@code userId} is null or blank.
     */
    void recordPaymentReceipt(String transactionId, long amount, String currency, String userId);

}