package dev.getelements.elements.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import dev.getelements.elements.stripe.service.StripeGateway;

/**
 * StripeGateway implementation for integration tests. Delegates directly to the
 * static Stripe SDK methods, relying on Stripe.apiKey already being set by the test.
 */
class LiveStripeGateway implements StripeGateway {

    @Override
    public Customer createCustomer(CustomerCreateParams params) throws StripeException {
        return Customer.create(params);
    }

    @Override
    public SetupIntent createSetupIntent(SetupIntentCreateParams params) throws StripeException {
        return SetupIntent.create(params);
    }

    @Override
    public PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params) throws StripeException {
        return Customer.retrieve(customerId).listPaymentMethods(params);
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
