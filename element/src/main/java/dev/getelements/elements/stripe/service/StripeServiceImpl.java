package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.billing.MeterEventCreateParams;
import com.stripe.param.billing.MeterListParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.CustomerListPaymentMethodsParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.billingportal.SessionCreateParams;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.dao.ReceiptDao;
import dev.getelements.elements.sdk.dao.Transaction;
import dev.getelements.elements.sdk.model.receipt.Receipt;
import dev.getelements.elements.sdk.service.user.UserService;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionRequest;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionResponse;
import dev.getelements.elements.stripe.model.CreateCustomerResponse;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreateSetupIntentResponse;
import dev.getelements.elements.stripe.model.CreateSubscriptionRequest;
import dev.getelements.elements.stripe.model.InvoiceSummary;
import dev.getelements.elements.stripe.model.MeterSummary;
import dev.getelements.elements.stripe.model.PaymentMethodSummary;
import dev.getelements.elements.stripe.model.PriceSummary;
import dev.getelements.elements.stripe.model.ProductSummary;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.inject.Provider;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ElementServiceExport(StripeService.class)
public class StripeServiceImpl implements StripeService {

    private static final String RECEIPT_SCHEMA = "stripe";

    /** Substring of Stripe's literal error text: {@code No active meter found for event name "<name>".} */
    private static final String NO_ACTIVE_METER_MESSAGE = "No active meter found for event name";

    private final StripeGateway gateway;
    private final StripeConfigService configService;
    private final StripePriceCache priceCache;
    private final StripeMeterPriceCache meterPriceCache;
    private final Provider<UserService> userServiceProvider;
    private final Provider<Transaction> transactionProvider;

    @Inject
    public StripeServiceImpl(
            StripeGateway gateway,
            StripeConfigService configService,
            StripePriceCache priceCache,
            StripeMeterPriceCache meterPriceCache,
            Provider<UserService> userServiceProvider,
            Provider<Transaction> transactionProvider) {
        this.gateway = gateway;
        this.configService = configService;
        this.priceCache = priceCache;
        this.meterPriceCache = meterPriceCache;
        this.userServiceProvider = userServiceProvider;
        this.transactionProvider = transactionProvider;
    }

    @Override
    public CreateCustomerResponse createCustomer(String email, String name, String orgId) {
        return createCustomer(email, name, orgId, configService.resolveDefaultMode());
    }

    @Override
    public CreateCustomerResponse createCustomer(String email, String name, String orgId, StripeMode mode) {

        try {

            final var params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .putMetadata(StripeService.METADATA_ORG_ID, orgId)
                    .build();

            return new CreateCustomerResponse(gateway.createCustomer(params, mode).getId());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateCustomer(String customerId, String email, String name) {
        updateCustomer(customerId, email, name, configService.resolveDefaultMode());
    }

    @Override
    public void updateCustomer(String customerId, String email, String name, StripeMode mode) {

        try {

            final var builder = CustomerUpdateParams.builder();
            if (email != null) builder.setEmail(email);
            if (name != null) builder.setName(name);
            gateway.updateCustomer(customerId, builder.build(), mode);

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public CreateSetupIntentResponse createSetupIntent(String customerId) {
        return createSetupIntent(customerId, configService.resolveDefaultMode());
    }

    @Override
    public CreateSetupIntentResponse createSetupIntent(String customerId, StripeMode mode) {

        try {

            final var params = SetupIntentCreateParams.builder()
                    .setCustomer(customerId)
                    .addPaymentMethodType("card")
                    .build();

            final var intent = gateway.createSetupIntent(params, mode);
            return new CreateSetupIntentResponse(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PaymentMethodSummary> listPaymentMethods(String customerId) {
        return listPaymentMethods(customerId, configService.resolveDefaultMode());
    }

    @Override
    public List<PaymentMethodSummary> listPaymentMethods(String customerId, StripeMode mode) {

        try {

            final var params = CustomerListPaymentMethodsParams.builder().build();

            return gateway.listPaymentMethods(customerId, params, mode).getData().stream()
                    .map(pm -> {
                        final var card = pm.getCard();
                        return new PaymentMethodSummary(
                                pm.getId(),
                                pm.getType(),
                                card != null ? card.getBrand() : null,
                                card != null ? card.getLast4() : null);
                    })
                    .toList();

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasPaymentMethod(String customerId) {
        return hasPaymentMethod(customerId, configService.resolveDefaultMode());
    }

    @Override
    public boolean hasPaymentMethod(String customerId, StripeMode mode) {

        try {

            final var params = CustomerListPaymentMethodsParams.builder().setLimit(1L).build();
            return !gateway.listPaymentMethods(customerId, params, mode).getData().isEmpty();

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public CreatePaymentIntentResponse createPaymentIntent(final CreatePaymentIntentRequest request) {
        return createPaymentIntent(request, configService.resolveDefaultMode());
    }

    @Override
    public CreatePaymentIntentResponse createPaymentIntent(final CreatePaymentIntentRequest request, StripeMode mode) {

        try {

            final var builder = PaymentIntentCreateParams.builder()
                    .setAmount(request.amount())
                    .setCurrency(request.currency())
                    .setCustomer(request.customerId())
                    .putMetadata(StripeService.METADATA_USER_ID, userServiceProvider.get().getCurrentUser().getId());

            if (request.description() != null) {
                builder.setDescription(request.description());
            }

            if (request.metadata() != null) {
                request.metadata().forEach(builder::putMetadata);
            }

            if (Boolean.TRUE.equals(request.automaticPaymentMethods())) {
                builder.setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build());
            }

            if (request.setupFutureUsage() != null) {
                builder.setSetupFutureUsage(
                        PaymentIntentCreateParams.SetupFutureUsage.valueOf(
                                request.setupFutureUsage().toUpperCase().replace('-', '_')));
            }

            final var intent = gateway.createPaymentIntent(builder.build(), request.idempotencyKey(), mode);
            return new CreatePaymentIntentResponse(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId) {
        return getSubscriptionStatus(subscriptionId, configService.resolveDefaultMode());
    }

    @Override
    public SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId, StripeMode mode) {

        try {

            final var sub = gateway.retrieveSubscription(subscriptionId, mode);
            return toStatusResponse(sub);

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public SubscriptionListResponse listSubscriptionsByCustomer(String customerId, String status, int limit, String startingAfter) {
        return listSubscriptionsByCustomer(customerId, status, limit, startingAfter, configService.resolveDefaultMode());
    }

    @Override
    public SubscriptionListResponse listSubscriptionsByCustomer(String customerId, String status, int limit, String startingAfter, StripeMode mode) {

        try {

            final var builder = SubscriptionListParams.builder()
                    .setCustomer(customerId)
                    .setLimit((long) limit);

            if (status != null && !status.isBlank()) {
                builder.setStatus(SubscriptionListParams.Status.valueOf(status.toUpperCase()));
            }

            if (startingAfter != null && !startingAfter.isBlank()) {
                builder.setStartingAfter(startingAfter);
            }

            final var collection = gateway.listSubscriptions(builder.build(), mode);

            final var subscriptions = collection.getData().stream()
                    .map(this::toStatusResponse)
                    .toList();

            final var nextCursor = subscriptions.isEmpty() ? null : subscriptions.getLast().subscriptionId();
            return new SubscriptionListResponse(subscriptions, Boolean.TRUE.equals(collection.getHasMore()), nextCursor);

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public String createBillingPortalSession(String customerId, String returnUrl) {
        return createBillingPortalSession(customerId, returnUrl, configService.resolveDefaultMode());
    }

    @Override
    public String createBillingPortalSession(String customerId, String returnUrl, StripeMode mode) {

        try {

            final var builder = SessionCreateParams.builder().setCustomer(customerId);

            if (returnUrl != null && !returnUrl.isBlank()) {
                builder.setReturnUrl(returnUrl);
            }

            return gateway.createBillingPortalSession(builder.build(), mode).getUrl();

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public SubscriptionStatusResponse createSubscription(String customerId, CreateSubscriptionRequest request) {
        return createSubscription(customerId, request, configService.resolveDefaultMode());
    }

    @Override
    public SubscriptionStatusResponse createSubscription(String customerId, CreateSubscriptionRequest request, StripeMode mode) {

        try {

            final var itemBuilder = SubscriptionCreateParams.Item.builder().setPrice(request.priceId());
            final var paramsBuilder = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(itemBuilder.build());

            if (request.description() != null) {
                paramsBuilder.setDescription(request.description());
            }

            if (request.metadata() != null) {
                request.metadata().forEach(paramsBuilder::putMetadata);
            }

            final var sub = gateway.createSubscription(paramsBuilder.build(), request.idempotencyKey(), mode);
            return toStatusResponse(sub);

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public SubscriptionStatusResponse cancelSubscription(String subscriptionId) {
        return cancelSubscription(subscriptionId, configService.resolveDefaultMode());
    }

    @Override
    public SubscriptionStatusResponse cancelSubscription(String subscriptionId, StripeMode mode) {
        try {
            return toStatusResponse(gateway.cancelSubscription(subscriptionId, mode));
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<ProductSummary> listProducts(boolean activeOnly, int limit) {
        return listProducts(activeOnly, limit, configService.resolveDefaultMode());
    }

    @Override
    public List<ProductSummary> listProducts(boolean activeOnly, int limit, StripeMode mode) {

        try {

            final var params = ProductListParams.builder()
                    .setActive(activeOnly)
                    .setLimit((long) limit)
                    .addExpand("data.default_price")
                    .build();

            return gateway.listProducts(params, mode).getData().stream()
                    .map(this::mapProduct)
                    .toList();

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public Optional<ProductSummary> getProduct(String productId) {
        return getProduct(productId, configService.resolveDefaultMode());
    }

    @Override
    public Optional<ProductSummary> getProduct(String productId, StripeMode mode) {

        try {
            return Optional.of(mapProduct(gateway.retrieveProduct(productId, mode)));
        } catch (InvalidRequestException e) {
            if ("resource_missing".equals(e.getCode())) {
                return Optional.empty();
            }
            throw stripeError(e);
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<PriceSummary> listPrices(String productId, boolean activeOnly, int limit) {
        return listPrices(productId, activeOnly, limit, configService.resolveDefaultMode());
    }

    @Override
    public List<PriceSummary> listPrices(String productId, boolean activeOnly, int limit, StripeMode mode) {

        final var cacheKey = mode.name() + "|" + productId + "|" + activeOnly + "|" + limit;
        final var cached = priceCache.get(cacheKey);

        if (cached != null) {
            return cached;
        }

        try {

            final var builder = PriceListParams.builder()
                    .setActive(activeOnly)
                    .setLimit((long) limit);

            if (productId != null && !productId.isBlank()) {
                builder.setProduct(productId);
            }

            final var prices = gateway.listPrices(builder.build(), mode).getData().stream()
                    .map(this::mapPrice)
                    .toList();

            priceCache.put(cacheKey, prices);
            return prices;

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public PriceSummary retrievePrice(String priceId) {
        return retrievePrice(priceId, configService.resolveDefaultMode());
    }

    @Override
    public PriceSummary retrievePrice(String priceId, StripeMode mode) {

        try {
            return mapPrice(gateway.retrievePrice(priceId, mode));
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<MeterSummary> listMeters(boolean activeOnly, int limit) {
        return listMeters(activeOnly, limit, configService.resolveDefaultMode());
    }

    @Override
    public List<MeterSummary> listMeters(boolean activeOnly, int limit, StripeMode mode) {
        try {
            return joinMetersToPrices(activeOnly, limit, mode);
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public Optional<PriceSummary> resolvePriceForMeterEventName(String eventName) {
        return resolvePriceForMeterEventName(eventName, (String) null);
    }

    @Override
    public Optional<PriceSummary> resolvePriceForMeterEventName(String eventName, StripeMode mode) {
        return resolvePriceForMeterEventName(eventName, null, mode);
    }

    @Override
    public Optional<PriceSummary> resolvePriceForMeterEventName(String eventName, String subscriptionId) {
        return resolvePriceForMeterEventName(eventName, subscriptionId, configService.resolveDefaultMode());
    }

    @Override
    public Optional<PriceSummary> resolvePriceForMeterEventName(String eventName, String subscriptionId, StripeMode mode) {

        if (subscriptionId != null && !subscriptionId.isBlank()) {
            try {
                return resolvePriceFromSubscription(eventName, subscriptionId, mode);
            } catch (StripeException e) {
                throw stripeError(e);
            }
        }

        final var cacheKey = mode.name() + "|" + eventName;
        final var cached = meterPriceCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            final var result = joinMetersToPrices(true, 100, mode).stream()
                    .filter(m -> eventName.equals(m.eventName()))
                    .findFirst()
                    .map(MeterSummary::price);
            meterPriceCache.put(cacheKey, result);
            return result;
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    /**
     * Resolves the meter for {@code eventName}, then matches it against {@code subscriptionId}'s
     * own line items rather than the catalogue-wide active-Price join, so a customer on a
     * non-default tier for a metered SKU gets the Price they're actually subscribed to. Not
     * cached — subscription item prices can change (upgrades/downgrades) independently of the
     * TTL that governs {@link #meterPriceCache}.
     */
    private Optional<PriceSummary> resolvePriceFromSubscription(String eventName, String subscriptionId, StripeMode mode) throws StripeException {

        final var meterId = findMeterIdForEventName(eventName, mode);

        if (meterId.isEmpty()) {
            return Optional.empty();
        }

        final var subscription = gateway.retrieveSubscription(subscriptionId, mode);

        return subscription.getItems().getData().stream()
                .map(com.stripe.model.SubscriptionItem::getPrice)
                .filter(p -> p.getRecurring() != null && meterId.get().equals(p.getRecurring().getMeter()))
                .findFirst()
                .map(this::mapPrice);
    }

    private Optional<String> findMeterIdForEventName(String eventName, StripeMode mode) throws StripeException {

        final var params = MeterListParams.builder()
                .setLimit(100L)
                .setStatus(MeterListParams.Status.ACTIVE)
                .build();

        return gateway.listMeters(params, mode).getData().stream()
                .filter(m -> eventName.equals(m.getEventName()))
                .map(com.stripe.model.billing.Meter::getId)
                .findFirst();
    }

    /**
     * Fetches meters (filtered per [activeOnly]/[limit]) and joins each to its active recurring
     * Price by {@code Price.Recurring#getMeter()} — Stripe has no "list prices by meter" endpoint,
     * so this is one extra round trip instead of one per meter. Last-write-wins if more than one
     * Price references the same meter. Shared by {@link #listMeters} and
     * {@link #resolvePriceForMeterEventName}.
     */
    private List<MeterSummary> joinMetersToPrices(boolean activeOnly, int limit, StripeMode mode) throws StripeException {

        final var builder = MeterListParams.builder().setLimit((long) limit);

        if (activeOnly) {
            builder.setStatus(MeterListParams.Status.ACTIVE);
        }

        final var meters = gateway.listMeters(builder.build(), mode).getData();

        final var priceListParams = PriceListParams.builder()
                .setActive(true)
                .setType(PriceListParams.Type.RECURRING)
                .setLimit(100L)
                .build();
        final var priceByMeterId = gateway.listPrices(priceListParams, mode).getData().stream()
                .filter(p -> p.getRecurring() != null && p.getRecurring().getMeter() != null)
                .collect(Collectors.toMap(p -> p.getRecurring().getMeter(), this::mapPrice, (a, b) -> b));

        return meters.stream()
                .map(m -> mapMeter(m, priceByMeterId.get(m.getId())))
                .toList();
    }

    @Override
    public Optional<String> findCustomerByMetadata(String metadataKey, String metadataValue) {
        return findCustomerByMetadata(metadataKey, metadataValue, configService.resolveDefaultMode());
    }

    @Override
    public Optional<String> findCustomerByMetadata(String metadataKey, String metadataValue, StripeMode mode) {

        try {

            final var query = "metadata['" + metadataKey + "']:'" + metadataValue + "'";
            final var params = CustomerSearchParams.builder()
                    .setQuery(query)
                    .setLimit(1L)
                    .build();

            return gateway.searchCustomers(params, mode).getData().stream()
                    .findFirst()
                    .map(Customer::getId);

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public CreateCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {
        return createCheckoutSession(request, configService.resolveDefaultMode());
    }

    @Override
    public CreateCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request, StripeMode mode) {

        try {

            final var checkoutMode = "payment".equalsIgnoreCase(request.mode())
                    ? com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT
                    : com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION;

            final var paramsBuilder = com.stripe.param.checkout.SessionCreateParams.builder()
                    .setCustomer(request.customerId())
                    .setMode(checkoutMode)
                    .setSuccessUrl(request.successUrl())
                    .setCancelUrl(request.cancelUrl())
                    .addLineItem(com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                            .setPrice(request.priceId())
                            .setQuantity(1L)
                            .build());

            if (request.metadata() != null && !request.metadata().isEmpty()) {
                request.metadata().forEach(paramsBuilder::putMetadata);
                if (checkoutMode == com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION) {
                    final var subData = com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder();
                    request.metadata().forEach(subData::putMetadata);
                    paramsBuilder.setSubscriptionData(subData.build());
                } else {
                    final var piData = com.stripe.param.checkout.SessionCreateParams.PaymentIntentData.builder();
                    request.metadata().forEach(piData::putMetadata);
                    paramsBuilder.setPaymentIntentData(piData.build());
                }
            }

            final var session = gateway.createCheckoutSession(paramsBuilder.build(), request.idempotencyKey(), mode);
            return new CreateCheckoutSessionResponse(session.getId(), session.getUrl());

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<InvoiceSummary> listInvoices(String customerId, int limit, String startingAfter) {
        return listInvoices(customerId, limit, startingAfter, configService.resolveDefaultMode());
    }

    @Override
    public List<InvoiceSummary> listInvoices(String customerId, int limit, String startingAfter, StripeMode mode) {

        try {

            final var builder = InvoiceListParams.builder()
                    .setCustomer(customerId)
                    .setLimit((long) limit);

            if (startingAfter != null && !startingAfter.isBlank()) {
                builder.setStartingAfter(startingAfter);
            }

            return gateway.listInvoices(builder.build(), mode).getData().stream()
                    .map(inv -> new InvoiceSummary(
                            inv.getId(),
                            inv.getSubscription(),
                            inv.getAmountPaid(),
                            inv.getCurrency(),
                            inv.getStatus(),
                            Instant.ofEpochSecond(inv.getCreated()).toString()))
                    .toList();

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public void recordMeterEvent(String customerId, String eventName, BigDecimal value, String idempotencyKey) {
        recordMeterEvent(customerId, eventName, value, idempotencyKey, configService.resolveDefaultMode());
    }

    @Override
    public void recordMeterEvent(String customerId, String eventName, BigDecimal value, String idempotencyKey, StripeMode mode) {

        try {

            final var params = MeterEventCreateParams.builder()
                    .setEventName(eventName)
                    .setIdentifier(idempotencyKey)
                    .putPayload("stripe_customer_id", customerId)
                    .putPayload("value", value.toPlainString())
                    .build();

            gateway.createMeterEvent(params, idempotencyKey, mode);

        } catch (StripeException e) {
            if (e.getMessage() != null && e.getMessage().contains(NO_ACTIVE_METER_MESSAGE)) {
                throw new NoSuchMeterException(eventName, e.getMessage());
            }
            throw stripeError(e);
        }
    }

    /** Maps a Stripe exception to the most appropriate JAX-RS exception. */
    private static RuntimeException stripeError(StripeException e) {
        if (e instanceof InvalidRequestException ire) {
            final var code = ire.getCode();
            if ("resource_missing".equals(code)) {
                return new NotFoundException(ire.getMessage());
            }
            return new BadRequestException(ire.getMessage());
        }
        return new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
    }

    @Override
    public void recordPaymentReceipt(String transactionId, long amount, String currency, String userId) {

        if (userId == null || userId.isBlank()) {
            return;
        }

        final var user = userServiceProvider.get().getUser(userId);
        final var receipt = new Receipt();
        receipt.setOriginalTransactionId(transactionId);
        receipt.setSchema(RECEIPT_SCHEMA);
        receipt.setUser(user);
        receipt.setPurchaseTime(System.currentTimeMillis());
        receipt.setBody(String.format(
                "{\"amount\":%d,\"currency\":\"%s\"}", amount, currency));

        transactionProvider.get().performAndCloseV(txn ->
                txn.getDao(ReceiptDao.class).createReceipt(receipt));
    }

    private SubscriptionStatusResponse toStatusResponse(com.stripe.model.Subscription sub) {
        final var periodEnd = sub.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(sub.getCurrentPeriodEnd()).toString()
                : null;
        return new SubscriptionStatusResponse(sub.getId(), sub.getStatus(), periodEnd);
    }

    private PriceSummary mapPrice(com.stripe.model.Price price) {
        final var recurring = price.getRecurring();
        return new PriceSummary(
                price.getId(),
                price.getProduct(),
                price.getNickname(),
                price.getUnitAmount(),
                price.getCurrency(),
                price.getType(),
                recurring != null ? recurring.getInterval() : null);
    }

    private ProductSummary mapProduct(com.stripe.model.Product product) {
        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getDescription(),
                Boolean.TRUE.equals(product.getActive()),
                product.getDefaultPriceObject() != null ? mapPrice(product.getDefaultPriceObject()) : null);
    }

    private MeterSummary mapMeter(com.stripe.model.billing.Meter meter, PriceSummary price) {
        return new MeterSummary(
                meter.getId(),
                meter.getDisplayName(),
                meter.getEventName(),
                meter.getStatus(),
                price);
    }

}
