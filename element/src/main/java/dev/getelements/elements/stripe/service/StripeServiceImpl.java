package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.ChargeListParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.CustomerListParams;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.InvoiceUpcomingParams;
import com.stripe.param.PaymentIntentListParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.billing.MeterEventCreateParams;
import com.stripe.param.billing.MeterEventSummaryListParams;
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
import dev.getelements.elements.stripe.model.ChargeSummary;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionRequest;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionResponse;
import dev.getelements.elements.stripe.model.CreateCustomerResponse;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreateSetupIntentResponse;
import dev.getelements.elements.stripe.model.CreateSubscriptionRequest;
import dev.getelements.elements.stripe.model.CustomerListResponse;
import dev.getelements.elements.stripe.model.CustomerSummary;
import dev.getelements.elements.stripe.model.CustomerUsageRollup;
import dev.getelements.elements.stripe.model.CustomerUsageTotal;
import dev.getelements.elements.stripe.model.InvoiceSummary;
import dev.getelements.elements.stripe.model.MeterSummary;
import dev.getelements.elements.stripe.model.MeterUsageSummary;
import dev.getelements.elements.stripe.model.PaymentIntentSummary;
import dev.getelements.elements.stripe.model.PaymentMethodSummary;
import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecord;
import dev.getelements.elements.stripe.model.PriceSummary;
import dev.getelements.elements.stripe.model.ProductSummary;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import dev.getelements.elements.stripe.model.UpcomingInvoiceSummary;
import jakarta.inject.Provider;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ElementServiceExport(StripeService.class)
public class StripeServiceImpl implements StripeService {

    private static final String RECEIPT_SCHEMA = "stripe";

    /** Substring of Stripe's literal error text: {@code No active meter found for event name "<name>".} */
    private static final String NO_ACTIVE_METER_MESSAGE = "No active meter found for event name";

    /** Time range used by {@link #listMeterUsage} when the caller supplies no explicit range. */
    private static final java.time.Duration DEFAULT_USAGE_WINDOW = java.time.Duration.ofHours(24);

    /** Stripe's per-request maximum for event summaries. */
    private static final long SUMMARIES_PAGE_LIMIT = 100;

    /** Hard cap on paged summaries so an over-wide window can't loop unboundedly. */
    private static final int SUMMARIES_HARD_CAP = 1000;

    /** Page size for customer listing during usage rollups. */
    private static final int CUSTOMER_PAGE_LIMIT = 100;

    /** Hard cap on customers included in a usage rollup. */
    private static final int CUSTOMER_ROLLUP_CAP = 1000;

    /** Concurrent Stripe reads during a usage rollup. */
    private static final int ROLLUP_PARALLELISM = 8;

    /** How long a computed usage rollup is reused (keyed by mode + window). */
    private static final long ROLLUP_CACHE_TTL_MS = 60_000;

    private final java.util.concurrent.ConcurrentHashMap<String, CachedRollup> rollupCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private final StripeGateway gateway;
    private final StripeConfigService configService;
    private final StripePriceCache priceCache;
    private final StripeMeterPriceCache meterPriceCache;
    private final Provider<UserService> userServiceProvider;
    private final Provider<Transaction> transactionProvider;
    private final MeterPostingService postingService;

    @Inject
    public StripeServiceImpl(
            StripeGateway gateway,
            StripeConfigService configService,
            StripePriceCache priceCache,
            StripeMeterPriceCache meterPriceCache,
            Provider<UserService> userServiceProvider,
            Provider<Transaction> transactionProvider,
            MeterPostingService postingService) {
        this.gateway = gateway;
        this.configService = configService;
        this.priceCache = priceCache;
        this.meterPriceCache = meterPriceCache;
        this.userServiceProvider = userServiceProvider;
        this.transactionProvider = transactionProvider;
        this.postingService = postingService;
    }

    @Override
    public StripeMode defaultMode() {
        return configService.resolveDefaultMode();
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
    public boolean hasPaymentMethod(String customerId, StripeMode mode) {

        try {

            final var params = CustomerListPaymentMethodsParams.builder().setLimit(1L).build();
            return !gateway.listPaymentMethods(customerId, params, mode).getData().isEmpty();

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
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
    public SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId, StripeMode mode) {

        try {

            final var sub = gateway.retrieveSubscription(subscriptionId, mode);
            return toStatusResponse(sub);

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
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
    public SubscriptionStatusResponse cancelSubscription(String subscriptionId, StripeMode mode) {
        try {
            return toStatusResponse(gateway.cancelSubscription(subscriptionId, mode));
        } catch (StripeException e) {
            throw stripeError(e);
        }
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
    public PriceSummary retrievePrice(String priceId, StripeMode mode) {

        try {
            return mapPrice(gateway.retrievePrice(priceId, mode));
        } catch (StripeException e) {
            throw stripeError(e);
        }
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
    public List<MeterUsageSummary> listMeterUsage(
            String eventName,
            String customerId,
            Instant startTime,
            Instant endTime,
            String window,
            StripeMode mode) {

        if (eventName == null || eventName.isBlank()) {
            throw new BadRequestException("eventName is required");
        }

        if (customerId == null || customerId.isBlank()) {
            throw new BadRequestException("customerId is required — Stripe's event summaries API only answers per-customer queries");
        }

        final var windowStrategy = parseWindow(window);
        final var resolvedEnd = align(endTime != null ? endTime : Instant.now(), windowStrategy);
        final var resolvedStart = align(startTime != null ? startTime : resolvedEnd.minus(DEFAULT_USAGE_WINDOW), windowStrategy);

        if (!resolvedStart.isBefore(resolvedEnd)) {
            throw new BadRequestException("startTime must be before endTime");
        }

        try {

            final var meterId = findMeterIdForEventName(eventName, mode)
                    .orElseThrow(() -> new NoSuchMeterException(eventName, "No active meter found for event name " + eventName));

            final var summaries = new ArrayList<MeterUsageSummary>();
            String startingAfter = null;

            // Page through Stripe's summaries (max 100/request) up to a hard cap so an over-wide
            // window can't loop unboundedly, but a normal hour/day bucketing always completes.
            do {

                final var builder = MeterEventSummaryListParams.builder()
                        .setCustomer(customerId)
                        .setStartTime(resolvedStart.getEpochSecond())
                        .setEndTime(resolvedEnd.getEpochSecond())
                        .setLimit(SUMMARIES_PAGE_LIMIT);

                if (windowStrategy != null) {
                    builder.setValueGroupingWindow(windowStrategy);
                }

                if (startingAfter != null) {
                    builder.setStartingAfter(startingAfter);
                }

                final var page = gateway.listMeterEventSummaries(meterId, builder.build(), mode);

                page.getData().forEach(s -> summaries.add(new MeterUsageSummary(
                        s.getId(),
                        s.getMeter(),
                        eventName,
                        s.getAggregatedValue(),
                        s.getStartTime() != null ? Instant.ofEpochSecond(s.getStartTime()).toString() : null,
                        s.getEndTime() != null ? Instant.ofEpochSecond(s.getEndTime()).toString() : null)));

                startingAfter = Boolean.TRUE.equals(page.getHasMore()) && !page.getData().isEmpty()
                        ? page.getData().getLast().getId()
                        : null;

            } while (startingAfter != null && summaries.size() < SUMMARIES_HARD_CAP);

            return summaries;

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    private static MeterEventSummaryListParams.ValueGroupingWindow parseWindow(String window) {
        if (window == null || window.isBlank()) {
            return null;
        }
        // Note: Stripe's API also supports a "day" grouping, but stripe-java 25.3.0's generated
        // ValueGroupingWindow enum predates it — add DAY there when the SDK is upgraded.
        return switch (window.trim().toLowerCase()) {
            case "hour" -> MeterEventSummaryListParams.ValueGroupingWindow.HOUR;
            default -> throw new BadRequestException("window must be \"hour\" or omitted for a single aggregate");
        };
    }

    /**
     * Aligns an instant down to the boundary Stripe requires for the given grouping window:
     * minutes for ungrouped queries and whole hours for {@code "hour"}. Flooring the (exclusive)
     * end can exclude the in-progress bucket — events also land asynchronously — which is the
     * conservative choice for verification sums.
     */
    private static Instant align(Instant instant, MeterEventSummaryListParams.ValueGroupingWindow window) {
        if (window == MeterEventSummaryListParams.ValueGroupingWindow.HOUR) {
            return instant.truncatedTo(ChronoUnit.HOURS);
        }
        return instant.truncatedTo(ChronoUnit.MINUTES);
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
    public CustomerUsageRollup getCustomerUsageTotals(int windowDays, boolean bypassCache, StripeMode mode) {

        final var clampedWindow = Math.max(1, Math.min(365, windowDays));
        final var cacheKey = mode.name() + "|" + clampedWindow;

        final var cached = rollupCache.get(cacheKey);
        if (!bypassCache && cached != null && System.currentTimeMillis() - cached.at() < ROLLUP_CACHE_TTL_MS) {
            return cached.rollup();
        }

        try {

            final var customers = new ArrayList<CustomerSummary>();
            String startingAfter = null;
            do {
                final var page = listCustomers(CUSTOMER_PAGE_LIMIT, startingAfter, mode);
                customers.addAll(page.customers());
                startingAfter = Boolean.TRUE.equals(page.hasMore()) ? page.nextCursor() : null;
            } while (startingAfter != null && customers.size() < CUSTOMER_ROLLUP_CAP);

            final var meters = gateway.listMeters(MeterListParams.builder()
                            .setLimit(100L)
                            .setStatus(MeterListParams.Status.ACTIVE)
                            .build(), mode)
                    .getData();

            final var start = Instant.now().minus(java.time.Duration.ofDays(clampedWindow))
                    .truncatedTo(ChronoUnit.MINUTES);
            final var end = Instant.now().truncatedTo(ChronoUnit.MINUTES);

            // Worker pool over a shared queue: bounded concurrent Stripe reads, one task per
            // customer, each summing every active meter for that customer.
            final var queue = new java.util.concurrent.ConcurrentLinkedQueue<>(customers);
            final var results = new java.util.concurrent.ConcurrentHashMap<String, CustomerUsageTotal>();

            try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                final var worker = new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            final var customer = queue.poll();
                            if (customer == null) {
                                return;
                            }
                            results.put(customer.id(), sumCustomerUsage(customer, meters, start, end, mode));
                        }
                    }
                };
                final var parallelism = Math.min(ROLLUP_PARALLELISM, Math.max(1, customers.size()));
                for (var i = 0; i < parallelism; i++) {
                    executor.submit(worker);
                }
            } // close() joins the workers

            final var totals = customers.stream()
                    .map(c -> results.get(c.id()))
                    .filter(Objects::nonNull)
                    .toList();

            final var rollup = new CustomerUsageRollup(totals, clampedWindow, Instant.now().toString());
            rollupCache.put(cacheKey, new CachedRollup(System.currentTimeMillis(), rollup));
            return rollup;

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    /**
     * Sums one customer's usage per active meter over the window. A failed meter aggregate
     * makes the customer's total {@code null} (unknown) — never zero, so verification logic
     * can't mistake an outage for inactivity.
     */
    private CustomerUsageTotal sumCustomerUsage(
            CustomerSummary customer,
            List<com.stripe.model.billing.Meter> meters,
            Instant start,
            Instant end,
            StripeMode mode) {

        var total = BigDecimal.ZERO;
        var unknown = false;
        final var byMeter = new java.util.LinkedHashMap<String, BigDecimal>();

        for (final var meter : meters) {

            try {

                final var builder = MeterEventSummaryListParams.builder()
                        .setCustomer(customer.id())
                        .setStartTime(start.getEpochSecond())
                        .setEndTime(end.getEpochSecond())
                        .setLimit(SUMMARIES_PAGE_LIMIT);

                var page = gateway.listMeterEventSummaries(meter.getId(), builder.build(), mode);
                var meterTotal = BigDecimal.ZERO;
                var hasMore = Boolean.TRUE.equals(page.getHasMore());
                var cursor = page.getData().isEmpty() ? null : page.getData().getLast().getId();

                for (final var summary : page.getData()) {
                    if (summary.getAggregatedValue() != null) {
                        meterTotal = meterTotal.add(summary.getAggregatedValue());
                    }
                }

                // Page through long windows (bounded) so totals aren't silently truncated.
                while (hasMore && cursor != null) {
                    page = gateway.listMeterEventSummaries(meter.getId(), builder
                            .setStartingAfter(cursor)
                            .build(), mode);
                    for (final var summary : page.getData()) {
                        if (summary.getAggregatedValue() != null) {
                            meterTotal = meterTotal.add(summary.getAggregatedValue());
                        }
                    }
                    hasMore = Boolean.TRUE.equals(page.getHasMore());
                    cursor = page.getData().isEmpty() ? null : page.getData().getLast().getId();
                }

                byMeter.put(meter.getEventName(), meterTotal);
                total = total.add(meterTotal);

            } catch (StripeException e) {
                unknown = true; // this meter's aggregate is unknown; the customer total reflects it
            }
        }

        return new CustomerUsageTotal(customer.id(), unknown ? null : total, byMeter);
    }

    private record CachedRollup(long at, CustomerUsageRollup rollup) {}

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
    public CustomerListResponse listCustomers(int limit, String startingAfter, StripeMode mode) {

        try {

            final var builder = CustomerListParams.builder()
                    .setLimit((long) limit);

            if (startingAfter != null && !startingAfter.isBlank()) {
                builder.setStartingAfter(startingAfter);
            }

            final var collection = gateway.listCustomers(builder.build(), mode);

            final var customers = collection.getData().stream()
                    .map(c -> new CustomerSummary(
                            c.getId(),
                            c.getName(),
                            c.getEmail(),
                            c.getMetadata() != null ? c.getMetadata() : java.util.Map.of(),
                            c.getCreated() != null ? Instant.ofEpochSecond(c.getCreated()).toString() : null,
                            Boolean.TRUE.equals(c.getLivemode())))
                    .toList();

            final var nextCursor = customers.isEmpty() ? null : customers.getLast().id();
            return new CustomerListResponse(customers, Boolean.TRUE.equals(collection.getHasMore()), nextCursor);

        } catch (StripeException e) {
            throw stripeError(e);
        }
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
    public List<ChargeSummary> listCharges(String customerId, int limit, String startingAfter, StripeMode mode) {

        try {

            final var builder = ChargeListParams.builder()
                    .setCustomer(customerId)
                    .setLimit((long) limit);

            if (startingAfter != null && !startingAfter.isBlank()) {
                builder.setStartingAfter(startingAfter);
            }

            return gateway.listCharges(builder.build(), mode).getData().stream()
                    .map(ch -> new ChargeSummary(
                            ch.getId(),
                            ch.getPaymentIntent(),
                            ch.getAmount(),
                            ch.getAmountRefunded(),
                            ch.getCurrency(),
                            ch.getStatus(),
                            ch.getDescription(),
                            ch.getReceiptUrl(),
                            Boolean.TRUE.equals(ch.getRefunded()),
                            ch.getCreated() != null ? Instant.ofEpochSecond(ch.getCreated()).toString() : null))
                    .toList();

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public Optional<UpcomingInvoiceSummary> getUpcomingInvoice(String customerId, StripeMode mode) {

        try {

            final var params = InvoiceUpcomingParams.builder()
                    .setCustomer(customerId)
                    .build();

            final var invoice = gateway.retrieveUpcomingInvoice(params, mode);

            final var lines = invoice.getLines().getData().stream()
                    .map(line -> new UpcomingInvoiceSummary.UpcomingInvoiceLine(
                            line.getDescription(),
                            line.getAmount() != null ? line.getAmount() : 0L,
                            line.getQuantity(),
                            line.getType()))
                    .toList();

            return Optional.of(new UpcomingInvoiceSummary(
                    invoice.getTotal(),
                    invoice.getCurrency(),
                    invoice.getPeriodStart() != null ? Instant.ofEpochSecond(invoice.getPeriodStart()).toString() : null,
                    invoice.getPeriodEnd() != null ? Instant.ofEpochSecond(invoice.getPeriodEnd()).toString() : null,
                    lines));

        } catch (InvalidRequestException e) {
            // No upcoming invoice is a normal state — no subscription and no pending usage.
            if (e.getStatusCode() != null && e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw stripeError(e);
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<PaymentIntentSummary> listPaymentIntents(String customerId, int limit, String startingAfter, StripeMode mode) {

        try {

            final var builder = PaymentIntentListParams.builder()
                    .setCustomer(customerId)
                    .setLimit((long) limit);

            if (startingAfter != null && !startingAfter.isBlank()) {
                builder.setStartingAfter(startingAfter);
            }

            return gateway.listPaymentIntents(builder.build(), mode).getData().stream()
                    .map(pi -> new PaymentIntentSummary(
                            pi.getId(),
                            pi.getAmount(),
                            pi.getCurrency(),
                            pi.getStatus(),
                            pi.getDescription(),
                            pi.getCreated() != null ? Instant.ofEpochSecond(pi.getCreated()).toString() : null))
                    .toList();

        } catch (StripeException e) {
            throw stripeError(e);
        }
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
            recordPostingQuietly(idempotencyKey, customerId, eventName, value, PostingOutcome.POSTED, mode, null);

        } catch (StripeException e) {
            final var detail = e.getMessage() != null ? e.getMessage()
                    : e.getStripeError() != null && e.getStripeError().getMessage() != null
                            ? e.getStripeError().getMessage()
                            : "Stripe error: " + e.getClass().getSimpleName();
            recordPostingQuietly(idempotencyKey, customerId, eventName, value, PostingOutcome.FAILED, mode, detail);
            if (isNoSuchMeterError(e)) {
                final var error = e.getStripeError();
                final var message = e.getMessage() != null
                        ? e.getMessage()
                        : error != null ? error.getMessage() : null;
                throw new NoSuchMeterException(eventName, message);
            }
            throw stripeError(e);
        } catch (RuntimeException e) {
            // stripe-java can throw a raw, message-less ClassCastException here instead of a
            // StripeException when Stripe's response body isn't shaped as the JSON object its own
            // error parser expects (see StripeMalformedResponseException for the full mechanism).
            // Without this catch, that exception propagates with zero context — not even which
            // event_name failed — making it effectively undiagnosable in logs.
            recordPostingQuietly(idempotencyKey, customerId, eventName, value, PostingOutcome.FAILED, mode,
                    "Unparseable Stripe response: " + e.getClass().getSimpleName());
            throw new StripeMalformedResponseException(
                    "recordMeterEvent(eventName='" + eventName + "', mode=" + mode + ") received a Stripe " +
                            "response this stripe-java version could not parse (" + e.getClass().getSimpleName() +
                            "). This has been observed when Stripe rejects an event_name with no matching " +
                            "active meter in the target account — verify a meter with this exact event name " +
                            "exists and is active in Stripe's " + mode + " dashboard.",
                    e);
        }
    }

    /**
     * Writes the posting-record entry for a meter-event attempt made through
     * {@link #recordMeterEvent}. Posting outcomes are recorded here automatically — callers only
     * need to record decisions that never reach this method (skips). A ledger failure is logged
     * and swallowed: the posting itself already succeeded or failed on its own terms, and the
     * caller's outcome must not depend on observability-store availability.
     */
    private void recordPostingQuietly(
            String idempotencyKey,
            String customerId,
            String eventName,
            BigDecimal value,
            PostingOutcome outcome,
            StripeMode mode,
            String detail) {
        try {
            postingService.recordPostingOutcome(new PostingRecord(
                    idempotencyKey, customerId, eventName, value, outcome, mode, detail, null));
        } catch (RuntimeException e) {
            System.getLogger(StripeServiceImpl.class.getName()).log(System.Logger.Level.WARNING,
                    "Failed to record posting outcome " + outcome + " for " + idempotencyKey, e);
        }
    }

    /**
     * Detects Stripe's "no active meter for event name" rejection, preferring the structured
     * {@link com.stripe.model.StripeError} payload (whose shape is stable across API versions)
     * over the top-level exception message, whose rendering has drifted between stripe-java
     * releases. Falls back to the exception message for older error paths that don't populate
     * the structured object.
     */
    private static boolean isNoSuchMeterError(StripeException e) {
        final var error = e.getStripeError();
        if (error != null && error.getMessage() != null
                && error.getMessage().contains(NO_ACTIVE_METER_MESSAGE)) {
            return true;
        }
        return e.getMessage() != null && e.getMessage().contains(NO_ACTIVE_METER_MESSAGE);
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
