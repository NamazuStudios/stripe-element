package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Product;
import com.stripe.model.ProductCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.billing.Meter;
import com.stripe.model.billing.MeterCollection;
import com.stripe.model.billing.MeterEvent;
import com.stripe.model.billingportal.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.ProductRetrieveParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.billing.MeterEventCreateParams;
import com.stripe.param.billing.MeterListParams;
import com.stripe.param.billingportal.SessionCreateParams;
import dev.getelements.elements.stripe.model.StripeMode;
import jakarta.ws.rs.InternalServerErrorException;

public class DefaultStripeGateway implements StripeGateway {

    private final StripeConfigService configService;

    @Inject
    private DefaultStripeGateway(StripeConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Customer createCustomer(CustomerCreateParams params) throws StripeException {
        return createCustomer(params, configService.resolveDefaultMode());
    }

    @Override
    public Customer createCustomer(CustomerCreateParams params, StripeMode mode) throws StripeException {
        return Customer.create(params, options(mode));
    }

    @Override
    public Customer updateCustomer(String customerId, CustomerUpdateParams params) throws StripeException {
        return updateCustomer(customerId, params, configService.resolveDefaultMode());
    }

    @Override
    public Customer updateCustomer(String customerId, CustomerUpdateParams params, StripeMode mode) throws StripeException {
        return Customer.retrieve(customerId, options(mode)).update(params, options(mode));
    }

    @Override
    public SetupIntent createSetupIntent(SetupIntentCreateParams params) throws StripeException {
        return createSetupIntent(params, configService.resolveDefaultMode());
    }

    @Override
    public SetupIntent createSetupIntent(SetupIntentCreateParams params, StripeMode mode) throws StripeException {
        return SetupIntent.create(params, options(mode));
    }

    @Override
    public PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params) throws StripeException {
        return listPaymentMethods(customerId, params, configService.resolveDefaultMode());
    }

    @Override
    public PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params, StripeMode mode) throws StripeException {
        return Customer.retrieve(customerId, options(mode)).listPaymentMethods(params, options(mode));
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, String idempotencyKey) throws StripeException {
        return createPaymentIntent(params, idempotencyKey, configService.resolveDefaultMode());
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException {
        return PaymentIntent.create(params, options(mode, idempotencyKey));
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return retrieveSubscription(subscriptionId, configService.resolveDefaultMode());
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId, StripeMode mode) throws StripeException {
        return Subscription.retrieve(subscriptionId, options(mode));
    }

    @Override
    public SubscriptionCollection listSubscriptions(SubscriptionListParams params) throws StripeException {
        return listSubscriptions(params, configService.resolveDefaultMode());
    }

    @Override
    public SubscriptionCollection listSubscriptions(SubscriptionListParams params, StripeMode mode) throws StripeException {
        return Subscription.list(params, options(mode));
    }

    @Override
    public Subscription createSubscription(SubscriptionCreateParams params, String idempotencyKey) throws StripeException {
        return createSubscription(params, idempotencyKey, configService.resolveDefaultMode());
    }

    @Override
    public Subscription createSubscription(SubscriptionCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException {
        return Subscription.create(params, options(mode, idempotencyKey));
    }

    @Override
    public Subscription cancelSubscription(String subscriptionId) throws StripeException {
        return cancelSubscription(subscriptionId, configService.resolveDefaultMode());
    }

    @Override
    public Subscription cancelSubscription(String subscriptionId, StripeMode mode) throws StripeException {
        return Subscription.retrieve(subscriptionId, options(mode)).cancel((SubscriptionCancelParams) null, options(mode));
    }

    @Override
    public Session createBillingPortalSession(SessionCreateParams params) throws StripeException {
        return createBillingPortalSession(params, configService.resolveDefaultMode());
    }

    @Override
    public Session createBillingPortalSession(SessionCreateParams params, StripeMode mode) throws StripeException {
        return Session.create(params, options(mode));
    }

    @Override
    public MeterEvent createMeterEvent(MeterEventCreateParams params, String idempotencyKey) throws StripeException {
        return createMeterEvent(params, idempotencyKey, configService.resolveDefaultMode());
    }

    @Override
    public MeterEvent createMeterEvent(MeterEventCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException {
        return MeterEvent.create(params, options(mode, idempotencyKey));
    }

    @Override
    public ProductCollection listProducts(ProductListParams params) throws StripeException {
        return listProducts(params, configService.resolveDefaultMode());
    }

    @Override
    public ProductCollection listProducts(ProductListParams params, StripeMode mode) throws StripeException {
        return Product.list(params, options(mode));
    }

    @Override
    public PriceCollection listPrices(PriceListParams params) throws StripeException {
        return listPrices(params, configService.resolveDefaultMode());
    }

    @Override
    public PriceCollection listPrices(PriceListParams params, StripeMode mode) throws StripeException {
        return Price.list(params, options(mode));
    }

    @Override
    public Price retrievePrice(String priceId) throws StripeException {
        return retrievePrice(priceId, configService.resolveDefaultMode());
    }

    @Override
    public Price retrievePrice(String priceId, StripeMode mode) throws StripeException {
        return Price.retrieve(priceId, options(mode));
    }

    @Override
    public Product retrieveProduct(String productId) throws StripeException {
        return retrieveProduct(productId, configService.resolveDefaultMode());
    }

    @Override
    public Product retrieveProduct(String productId, StripeMode mode) throws StripeException {
        final var params = ProductRetrieveParams.builder()
                .addExpand("default_price")
                .build();
        return Product.retrieve(productId, params, options(mode));
    }

    @Override
    public MeterCollection listMeters(MeterListParams params) throws StripeException {
        return listMeters(params, configService.resolveDefaultMode());
    }

    @Override
    public MeterCollection listMeters(MeterListParams params, StripeMode mode) throws StripeException {
        return Meter.list(params, options(mode));
    }

    @Override
    public CustomerSearchResult searchCustomers(CustomerSearchParams params) throws StripeException {
        return searchCustomers(params, configService.resolveDefaultMode());
    }

    @Override
    public CustomerSearchResult searchCustomers(CustomerSearchParams params, StripeMode mode) throws StripeException {
        return Customer.search(params, options(mode));
    }

    @Override
    public InvoiceCollection listInvoices(InvoiceListParams params) throws StripeException {
        return listInvoices(params, configService.resolveDefaultMode());
    }

    @Override
    public InvoiceCollection listInvoices(InvoiceListParams params, StripeMode mode) throws StripeException {
        return Invoice.list(params, options(mode));
    }

    @Override
    public com.stripe.model.checkout.Session createCheckoutSession(
            com.stripe.param.checkout.SessionCreateParams params,
            String idempotencyKey) throws StripeException {
        return createCheckoutSession(params, idempotencyKey, configService.resolveDefaultMode());
    }

    @Override
    public com.stripe.model.checkout.Session createCheckoutSession(
            com.stripe.param.checkout.SessionCreateParams params,
            String idempotencyKey,
            StripeMode mode) throws StripeException {
        return com.stripe.model.checkout.Session.create(params, options(mode, idempotencyKey));
    }

    private RequestOptions options(StripeMode mode) {
        return options(mode, null);
    }

    private RequestOptions options(StripeMode mode, String idempotencyKey) {

        final var apiKey = configService.getConfig(mode).apiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new InternalServerErrorException("Stripe API key not configured for mode " + mode);
        }

        final var builder = RequestOptions.builder().setApiKey(apiKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.setIdempotencyKey(idempotencyKey);
        }

        return builder.build();
    }

}
