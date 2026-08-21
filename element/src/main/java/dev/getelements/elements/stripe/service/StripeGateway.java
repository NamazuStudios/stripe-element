package dev.getelements.elements.stripe.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Product;
import com.stripe.model.ProductCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.billing.MeterCollection;
import com.stripe.model.billing.MeterEvent;
import com.stripe.model.billingportal.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.billing.MeterEventCreateParams;
import com.stripe.param.billing.MeterListParams;
import com.stripe.param.billingportal.SessionCreateParams;
import dev.getelements.elements.stripe.model.StripeMode;

/**
 * Boundary to the Stripe SDK. Every method has a mode-aware overload taking an explicit
 * {@link StripeMode}; the original no-mode overloads are preserved for backwards compatibility
 * and resolve their mode via {@link StripeConfigService#resolveDefaultMode()}.
 */
public interface StripeGateway {

    Customer createCustomer(CustomerCreateParams params) throws StripeException;

    Customer createCustomer(CustomerCreateParams params, StripeMode mode) throws StripeException;

    Customer updateCustomer(String customerId, CustomerUpdateParams params) throws StripeException;

    Customer updateCustomer(String customerId, CustomerUpdateParams params, StripeMode mode) throws StripeException;

    SetupIntent createSetupIntent(SetupIntentCreateParams params) throws StripeException;

    SetupIntent createSetupIntent(SetupIntentCreateParams params, StripeMode mode) throws StripeException;

    PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params) throws StripeException;

    PaymentMethodCollection listPaymentMethods(String customerId, CustomerListPaymentMethodsParams params, StripeMode mode) throws StripeException;

    PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, String idempotencyKey) throws StripeException;

    PaymentIntent createPaymentIntent(PaymentIntentCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException;

    Subscription retrieveSubscription(String subscriptionId) throws StripeException;

    Subscription retrieveSubscription(String subscriptionId, StripeMode mode) throws StripeException;

    SubscriptionCollection listSubscriptions(SubscriptionListParams params) throws StripeException;

    SubscriptionCollection listSubscriptions(SubscriptionListParams params, StripeMode mode) throws StripeException;

    Subscription createSubscription(SubscriptionCreateParams params, String idempotencyKey) throws StripeException;

    Subscription createSubscription(SubscriptionCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException;

    Subscription cancelSubscription(String subscriptionId) throws StripeException;

    Subscription cancelSubscription(String subscriptionId, StripeMode mode) throws StripeException;

    Session createBillingPortalSession(SessionCreateParams params) throws StripeException;

    Session createBillingPortalSession(SessionCreateParams params, StripeMode mode) throws StripeException;

    MeterEvent createMeterEvent(MeterEventCreateParams params, String idempotencyKey) throws StripeException;

    MeterEvent createMeterEvent(MeterEventCreateParams params, String idempotencyKey, StripeMode mode) throws StripeException;

    ProductCollection listProducts(ProductListParams params) throws StripeException;

    ProductCollection listProducts(ProductListParams params, StripeMode mode) throws StripeException;

    PriceCollection listPrices(PriceListParams params) throws StripeException;

    PriceCollection listPrices(PriceListParams params, StripeMode mode) throws StripeException;

    Price retrievePrice(String priceId) throws StripeException;

    Price retrievePrice(String priceId, StripeMode mode) throws StripeException;

    Product retrieveProduct(String productId) throws StripeException;

    Product retrieveProduct(String productId, StripeMode mode) throws StripeException;

    MeterCollection listMeters(MeterListParams params) throws StripeException;

    MeterCollection listMeters(MeterListParams params, StripeMode mode) throws StripeException;

    CustomerSearchResult searchCustomers(CustomerSearchParams params) throws StripeException;

    CustomerSearchResult searchCustomers(CustomerSearchParams params, StripeMode mode) throws StripeException;

    InvoiceCollection listInvoices(InvoiceListParams params) throws StripeException;

    InvoiceCollection listInvoices(InvoiceListParams params, StripeMode mode) throws StripeException;

    com.stripe.model.checkout.Session createCheckoutSession(
            com.stripe.param.checkout.SessionCreateParams params,
            String idempotencyKey) throws StripeException;

    com.stripe.model.checkout.Session createCheckoutSession(
            com.stripe.param.checkout.SessionCreateParams params,
            String idempotencyKey,
            StripeMode mode) throws StripeException;

}
