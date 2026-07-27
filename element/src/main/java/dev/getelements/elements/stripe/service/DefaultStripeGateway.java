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
    public Customer updateCustomer(String customerId, CustomerUpdateParams params) throws StripeException {
        return Customer.retrieve(customerId, options()).update(params, options());
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
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, String idempotencyKey) throws StripeException {
        return PaymentIntent.create(params, options(idempotencyKey));
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId, options());
    }

    @Override
    public SubscriptionCollection listSubscriptions(SubscriptionListParams params) throws StripeException {
        return Subscription.list(params, options());
    }

    @Override
    public Subscription createSubscription(SubscriptionCreateParams params, String idempotencyKey) throws StripeException {
        return Subscription.create(params, options(idempotencyKey));
    }

    @Override
    public Subscription cancelSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId, options()).cancel((SubscriptionCancelParams) null, options());
    }

    @Override
    public Session createBillingPortalSession(SessionCreateParams params) throws StripeException {
        return Session.create(params, options());
    }

    @Override
    public MeterEvent createMeterEvent(MeterEventCreateParams params, String idempotencyKey) throws StripeException {
        return MeterEvent.create(params, options(idempotencyKey));
    }

    @Override
    public ProductCollection listProducts(ProductListParams params) throws StripeException {
        return Product.list(params, options());
    }

    @Override
    public PriceCollection listPrices(PriceListParams params) throws StripeException {
        return Price.list(params, options());
    }

    @Override
    public Price retrievePrice(String priceId) throws StripeException {
        return Price.retrieve(priceId, options());
    }

    @Override
    public Product retrieveProduct(String productId) throws StripeException {
        final var params = ProductRetrieveParams.builder()
                .addExpand("default_price")
                .build();
        return Product.retrieve(productId, params, options());
    }

    @Override
    public MeterCollection listMeters(MeterListParams params) throws StripeException {
        return Meter.list(params, options());
    }

    @Override
    public CustomerSearchResult searchCustomers(CustomerSearchParams params) throws StripeException {
        return Customer.search(params, options());
    }

    @Override
    public InvoiceCollection listInvoices(InvoiceListParams params) throws StripeException {
        return Invoice.list(params, options());
    }

    @Override
    public com.stripe.model.checkout.Session createCheckoutSession(
            com.stripe.param.checkout.SessionCreateParams params,
            String idempotencyKey) throws StripeException {
        return com.stripe.model.checkout.Session.create(params, options(idempotencyKey));
    }

    private RequestOptions options() {
        return options(null);
    }

    private RequestOptions options(String idempotencyKey) {

        final var apiKey = configService.getConfig().apiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new InternalServerErrorException("Stripe API key not configured");
        }

        final var builder = RequestOptions.builder().setApiKey(apiKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.setIdempotencyKey(idempotencyKey);
        }

        return builder.build();
    }

}
