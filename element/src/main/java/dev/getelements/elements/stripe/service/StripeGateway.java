package dev.getelements.elements.stripe.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.SetupIntentCreateParams;

public interface StripeGateway {

    Customer createCustomer(CustomerCreateParams params) throws StripeException;

    SetupIntent createSetupIntent(SetupIntentCreateParams params) throws StripeException;

    PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params) throws StripeException;

    PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException;

    Subscription retrieveSubscription(String subscriptionId) throws StripeException;

}
