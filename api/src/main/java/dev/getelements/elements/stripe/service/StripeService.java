package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.model.CreateCheckoutSessionRequest;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionResponse;
import dev.getelements.elements.stripe.model.CreateCustomerResponse;
import dev.getelements.elements.stripe.model.CreateSubscriptionRequest;
import dev.getelements.elements.stripe.model.InvoiceSummary;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreateSetupIntentResponse;
import dev.getelements.elements.stripe.model.MeterSummary;
import dev.getelements.elements.stripe.model.PaymentMethodSummary;
import dev.getelements.elements.stripe.model.PriceSummary;
import dev.getelements.elements.stripe.model.ProductSummary;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;

import java.util.List;
import java.util.Optional;

public interface StripeService {

    /** Metadata key used to associate a Stripe customer/payment with a platform user. */
    String METADATA_USER_ID = "userId";

    /** Metadata key used to associate a Stripe customer/subscription with an org. */
    String METADATA_ORG_ID = "orgId";

    /**
     * Creates a Stripe Customer and returns its ID. The {@code orgId} is stored in the customer's
     * metadata under {@link #METADATA_ORG_ID} so subsequent webhook events can be reverse-looked-up
     * to the correct org.
     *
     * @param email  customer email address
     * @param name   customer display name
     * @param orgId  platform org ID to store in Stripe metadata
     */
    CreateCustomerResponse createCustomer(String email, String name, String orgId);

    /**
     * Creates a Stripe SetupIntent for the given customer and returns the client secret.
     * The frontend passes this secret to Stripe.js ({@code stripe.confirmCardSetup}) to
     * collect and attach a payment method without an immediate charge. Use this before
     * {@link #createSubscription} or any deferred payment flow.
     *
     * @param customerId Stripe customer ID
     */
    CreateSetupIntentResponse createSetupIntent(String customerId);

    /**
     * Lists the card payment methods currently attached to a customer. Useful for displaying
     * saved cards or checking whether a customer has a payment method on file before
     * creating a subscription.
     *
     * @param customerId Stripe customer ID
     */
    List<PaymentMethodSummary> listPaymentMethods(String customerId);

    /**
     * Checks whether {@code customerId} has at least one payment method on file, without
     * materializing the full list. Prefer this over {@code !listPaymentMethods(customerId).isEmpty()}
     * for billing-readiness checks that only need a boolean.
     *
     * @param customerId Stripe customer ID
     */
    boolean hasPaymentMethod(String customerId);

    /**
     * Creates a Stripe PaymentIntent for a <strong>one-off purchase</strong> and returns the
     * client secret. The frontend passes this secret to Stripe.js ({@code stripe.confirmCardPayment})
     * to complete the charge. For recurring charges use {@link #createSubscription} instead.
     *
     * @param request charge parameters including amount (in smallest currency unit), currency,
     *                and the Stripe customer ID
     */
    CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request);

    /**
     * Creates a <strong>recurring subscription</strong> for the given customer at the given price.
     * The customer must already have a default payment method on file (attached via
     * {@link #createSetupIntent}). Stripe will charge the customer immediately and then on each
     * renewal interval defined by the price.
     *
     * @param customerId Stripe customer ID
     * @param request    subscription parameters including price, optional metadata, and idempotency key
     */
    SubscriptionStatusResponse createSubscription(String customerId, CreateSubscriptionRequest request);

    /**
     * Retrieves the current status of a single Stripe subscription.
     *
     * @param subscriptionId Stripe subscription ID ({@code sub_...})
     */
    SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId);

    /**
     * Lists subscriptions for the given Stripe customer ID, newest first.
     *
     * @param customerId    Stripe customer ID
     * @param status        Stripe status filter (e.g. {@code "active"}, {@code "canceled"},
     *                      {@code "all"}); {@code null} uses Stripe's default (non-canceled only)
     * @param limit         maximum results to return (1–100)
     * @param startingAfter subscription ID cursor for the next page; {@code null} for the first page
     */
    SubscriptionListResponse listSubscriptionsByCustomer(String customerId, String status, int limit, String startingAfter);

    /**
     * Lists products from the Stripe catalogue.
     *
     * @param activeOnly if {@code true}, only active (non-archived) products are returned
     * @param limit      maximum number of products to return (1–100)
     */
    List<ProductSummary> listProducts(boolean activeOnly, int limit);

    /**
     * Retrieves a single Stripe product by its ID, including its default price. Returns
     * {@link Optional#empty()} if no product with that ID exists (rather than throwing), so callers
     * that already treat a missing product as a normal (if unwanted) precondition failure don't need
     * to catch an exception. Prefer this over paging through {@link #listProducts} and filtering by
     * ID — it also avoids missing a product that falls outside the requested page.
     *
     * @param productId Stripe product ID ({@code prod_...})
     */
    Optional<ProductSummary> getProduct(String productId);

    /**
     * Lists prices from the Stripe product catalogue. Results are cached in memory for
     * {@code dev.getelements.elements.stripe.price.cache.ttl.ms} milliseconds (default 5 minutes)
     * to avoid redundant Stripe API calls on every page load.
     *
     * @param productId  optional Stripe product ID to filter by ({@code prod_...});
     *                   {@code null} returns prices across all products
     * @param activeOnly if {@code true}, only active (non-archived) prices are returned
     * @param limit      maximum number of prices to return (1–100)
     */
    List<PriceSummary> listPrices(String productId, boolean activeOnly, int limit);

    /**
     * Retrieves a single Stripe price by its ID. Use this to look up current pricing without
     * knowing the product ID — useful for displaying the cost of an existing subscription or SKU.
     *
     * @param priceId Stripe price ID ({@code price_...})
     */
    PriceSummary retrievePrice(String priceId);

    /**
     * Lists Billing Meters from the Stripe account. A meter's {@link MeterSummary#eventName()} is
     * the value catalogue configuration (e.g. a metered SKU's meter event name) should store to
     * associate a billable unit with usage recorded via {@link #recordMeterEvent}.
     *
     * @param activeOnly if {@code true}, only active meters are returned
     * @param limit      maximum number of meters to return (1–100)
     */
    List<MeterSummary> listMeters(boolean activeOnly, int limit);

    /**
     * Searches for a Stripe customer by a metadata key-value pair using the Stripe Customer Search API.
     * Returns the customer's Stripe ID if exactly one match is found, or an empty {@link Optional} if
     * no customer has that metadata. Use {@code metadataKey = }{@link #METADATA_ORG_ID} to prevent
     * creating duplicate customers when an org is deleted and recreated.
     *
     * <p><strong>Note:</strong> key and value must not contain single-quote characters.
     *
     * @param metadataKey   the metadata field name (e.g. {@code "orgId"})
     * @param metadataValue the value to search for
     * @return the Stripe customer ID, or empty if none found
     */
    Optional<String> findCustomerByMetadata(String metadataKey, String metadataValue);

    /**
     * Creates a Stripe-hosted Checkout Session and returns the redirect URL. The customer is sent
     * to this URL where Stripe handles payment method collection and confirmation. After the customer
     * completes or cancels, Stripe redirects to {@code successUrl} or {@code cancelUrl}.
     *
     * <p>If {@link CreateCheckoutSessionRequest#mode()} is {@code null} or absent, defaults to
     * {@code subscription} mode. Use {@code payment} for one-off charges.
     *
     * @param request checkout parameters including customer, price, redirect URLs, and mode
     */
    CreateCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request);

    /**
     * Creates a Stripe Customer Portal session and returns the single-use URL. The customer
     * can use the portal to manage their subscriptions and payment methods without any
     * server-side subscription logic in the game. The URL expires after a short period.
     *
     * @param customerId the Stripe customer ID
     * @param returnUrl  optional URL to redirect the customer back to after leaving the portal;
     *                   {@code null} uses the portal's configured default
     * @return single-use portal session URL
     */
    String createBillingPortalSession(String customerId, String returnUrl);

    /**
     * Records a billing meter event for usage-based billing. The {@code idempotencyKey} is
     * passed to Stripe both as the event {@code identifier} (Stripe's native meter-event
     * deduplication) and as the HTTP idempotency key, so retries never produce a double-charge.
     *
     * @param customerId     Stripe customer ID
     * @param eventName      meter name as configured in the Stripe Dashboard
     * @param value          usage quantity to report (must be &gt; 0)
     * @param idempotencyKey unique key for this event; re-submitting the same key is a no-op
     * @throws NoSuchMeterException if Stripe has no active meter configured for {@code eventName}
     */
    void recordMeterEvent(String customerId, String eventName, long value, String idempotencyKey);

    /**
     * Updates a Stripe customer's contact details. Fields that are {@code null} are left unchanged.
     *
     * @param customerId Stripe customer ID
     * @param email      new email address, or {@code null} to leave unchanged
     * @param name       new display name, or {@code null} to leave unchanged
     */
    void updateCustomer(String customerId, String email, String name);

    /**
     * Immediately cancels a Stripe subscription and returns its final status.
     * The customer loses access at the moment of cancellation. For end-of-period cancellation,
     * use the Customer Portal instead.
     *
     * @param subscriptionId Stripe subscription ID ({@code sub_...})
     */
    SubscriptionStatusResponse cancelSubscription(String subscriptionId);

    /**
     * Lists invoices for the given Stripe customer, newest first.
     *
     * @param customerId    Stripe customer ID
     * @param limit         maximum results to return (1–100)
     * @param startingAfter invoice ID cursor for the next page; {@code null} for the first page
     */
    List<InvoiceSummary> listInvoices(String customerId, int limit, String startingAfter);

    /**
     * Records a receipt for a confirmed payment in the platform receipt store. Called internally
     * from the webhook handler after Stripe fires {@code payment_intent.succeeded} or
     * {@code invoice.payment_succeeded}. Silently skips if {@code userId} is {@code null} or blank.
     *
     * @param transactionId Stripe payment intent or invoice ID
     * @param amount        amount in the smallest currency unit (e.g. cents)
     * @param currency      ISO 4217 currency code
     * @param userId        platform user ID to attach the receipt to
     */
    void recordPaymentReceipt(String transactionId, long amount, String currency, String userId);

}
