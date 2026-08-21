package dev.getelements.elements.stripe;

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
import dev.getelements.elements.stripe.service.StripeGateway;

/**
 * StripeGateway implementation for integration tests. Delegates directly to the
 * static Stripe SDK methods, relying on Stripe.apiKey already being set by the test.
 * Mode is ignored — the test's global Stripe.apiKey already picks the account.
 */
class LiveStripeGateway implements StripeGateway {

    @Override
    public Customer createCustomer(CustomerCreateParams params) throws StripeException {
        return Customer.create(params);
    }

    @Override
    public Customer createCustomer(CustomerCreateParams params, StripeMode mode) throws StripeException {
        return createCustomer(params);
    }

    @Override
    public Customer updateCustomer(String customerId, CustomerUpdateParams params) throws StripeException {
        return Customer.retrieve(customerId).update(params);
    }

    @Override
    public Customer updateCustomer(String customerId, CustomerUpdateParams params, StripeMode mode) throws StripeException {
        return updateCustomer(customerId, params);
    }

    @Override
    public SetupIntent createSetupIntent(SetupIntentCreateParams params) throws StripeException {
        return SetupIntent.create(params);
    }

    @Override
    public SetupIntent createSetupIntent(SetupIntentCreateParams params, StripeMode mode) throws StripeException {
        return createSetupIntent(params);
    }

    @Override
    public PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params) throws StripeException {
        return Customer.retrieve(customerId).listPaymentMethods(params);
    }

    @Override
    public PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params, StripeMode mode) throws StripeException {
        return listPaymentMethods(customerId, params);
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, String idempotencyKey) throws StripeException {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return PaymentIntent.create(params, RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
        }
        return PaymentIntent.create(params);
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException {
        return createPaymentIntent(params, idempotencyKey);
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

    @Override
    public Subscription retrieveSubscription(String subscriptionId, StripeMode mode) throws StripeException {
        return retrieveSubscription(subscriptionId);
    }

    @Override
    public SubscriptionCollection listSubscriptions(SubscriptionListParams params) throws StripeException {
        return Subscription.list(params);
    }

    @Override
    public SubscriptionCollection listSubscriptions(SubscriptionListParams params, StripeMode mode) throws StripeException {
        return listSubscriptions(params);
    }

    @Override
    public Subscription createSubscription(SubscriptionCreateParams params, String idempotencyKey) throws StripeException {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return Subscription.create(params, RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
        }
        return Subscription.create(params);
    }

    @Override
    public Subscription createSubscription(SubscriptionCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException {
        return createSubscription(params, idempotencyKey);
    }

    @Override
    public Subscription cancelSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId).cancel((SubscriptionCancelParams) null);
    }

    @Override
    public Subscription cancelSubscription(String subscriptionId, StripeMode mode) throws StripeException {
        return cancelSubscription(subscriptionId);
    }

    @Override
    public Session createBillingPortalSession(SessionCreateParams params) throws StripeException {
        return Session.create(params);
    }

    @Override
    public Session createBillingPortalSession(SessionCreateParams params, StripeMode mode) throws StripeException {
        return createBillingPortalSession(params);
    }

    @Override
    public MeterEvent createMeterEvent(MeterEventCreateParams params, String idempotencyKey) throws StripeException {
        final var options = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();
        return MeterEvent.create(params, options);
    }

    @Override
    public MeterEvent createMeterEvent(MeterEventCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException {
        return createMeterEvent(params, idempotencyKey);
    }

    @Override
    public ProductCollection listProducts(ProductListParams params) throws StripeException {
        return Product.list(params);
    }

    @Override
    public ProductCollection listProducts(ProductListParams params, StripeMode mode) throws StripeException {
        return listProducts(params);
    }

    @Override
    public PriceCollection listPrices(PriceListParams params) throws StripeException {
        return Price.list(params);
    }

    @Override
    public PriceCollection listPrices(PriceListParams params, StripeMode mode) throws StripeException {
        return listPrices(params);
    }

    @Override
    public Product retrieveProduct(String productId) throws StripeException {
        final var params = ProductRetrieveParams.builder()
                .addExpand("default_price")
                .build();
        return Product.retrieve(productId, params, RequestOptions.getDefault());
    }

    @Override
    public Product retrieveProduct(String productId, StripeMode mode) throws StripeException {
        return retrieveProduct(productId);
    }

    @Override
    public Price retrievePrice(String priceId) throws StripeException {
        return Price.retrieve(priceId);
    }

    @Override
    public Price retrievePrice(String priceId, StripeMode mode) throws StripeException {
        return retrievePrice(priceId);
    }

    @Override
    public MeterCollection listMeters(MeterListParams params) throws StripeException {
        return Meter.list(params);
    }

    @Override
    public MeterCollection listMeters(MeterListParams params, StripeMode mode) throws StripeException {
        return listMeters(params);
    }

    @Override
    public CustomerSearchResult searchCustomers(CustomerSearchParams params) throws StripeException {
        return Customer.search(params);
    }

    @Override
    public CustomerSearchResult searchCustomers(CustomerSearchParams params, StripeMode mode) throws StripeException {
        return searchCustomers(params);
    }

    @Override
    public InvoiceCollection listInvoices(InvoiceListParams params) throws StripeException {
        return Invoice.list(params);
    }

    @Override
    public InvoiceCollection listInvoices(InvoiceListParams params, StripeMode mode) throws StripeException {
        return listInvoices(params);
    }

    @Override
    public com.stripe.model.checkout.Session createCheckoutSession(
            com.stripe.param.checkout.SessionCreateParams params,
            String idempotencyKey) throws StripeException {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return com.stripe.model.checkout.Session.create(
                    params, RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
        }
        return com.stripe.model.checkout.Session.create(params);
    }

    @Override
    public com.stripe.model.checkout.Session createCheckoutSession(
            com.stripe.param.checkout.SessionCreateParams params,
            String idempotencyKey,
            StripeMode mode) throws StripeException {
        return createCheckoutSession(params, idempotencyKey);
    }

}
