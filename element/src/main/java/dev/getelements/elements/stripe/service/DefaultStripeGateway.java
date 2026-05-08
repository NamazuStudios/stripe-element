package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.SetupIntentCreateParams;
import jakarta.ws.rs.InternalServerErrorException;

public class DefaultStripeGateway implements StripeGateway {

    private final StripeConfigService configService;

    @Inject
    private DefaultStripeGateway(StripeConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Customer createCustomer(CustomerCreateParams params) throws StripeException {
        return Customer.create(params, options());
    }

    @Override
    public SetupIntent createSetupIntent(SetupIntentCreateParams params) throws StripeException {
        return SetupIntent.create(params, options());
    }

    @Override
    public PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params) throws StripeException {
        return Customer.retrieve(customerId, options()).listPaymentMethods(params, options());
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException {
        return PaymentIntent.create(params, options());
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId, options());
    }

    private RequestOptions options() {

        final var apiKey = configService.getConfig().apiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new InternalServerErrorException("Stripe API key not configured");
        }

        return RequestOptions.builder().setApiKey(apiKey).build();
    }

}
