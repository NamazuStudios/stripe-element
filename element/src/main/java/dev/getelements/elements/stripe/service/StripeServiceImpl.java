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
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.inject.Provider;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ElementServiceExport(StripeService.class)
public class StripeServiceImpl implements StripeService {

    private static final String RECEIPT_SCHEMA = "stripe";

    private final StripeGateway gateway;
    private final StripePriceCache priceCache;
    private final Provider<UserService> userServiceProvider;
    private final Provider<Transaction> transactionProvider;

    @Inject
    public StripeServiceImpl(
            StripeGateway gateway,
            StripePriceCache priceCache,
            Provider<UserService> userServiceProvider,
            Provider<Transaction> transactionProvider) {
        this.gateway = gateway;
        this.priceCache = priceCache;
        this.userServiceProvider = userServiceProvider;
        this.transactionProvider = transactionProvider;
    }

    @Override
    public CreateCustomerResponse createCustomer(String email, String name, String orgId) {

        try {

            final var params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .putMetadata(StripeService.METADATA_ORG_ID, orgId)
                    .build();

            return new CreateCustomerResponse(gateway.createCustomer(params).getId());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateCustomer(String customerId, String email, String name) {

        try {

            final var builder = CustomerUpdateParams.builder();
            if (email != null) builder.setEmail(email);
            if (name != null) builder.setName(name);
            gateway.updateCustomer(customerId, builder.build());

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public CreateSetupIntentResponse createSetupIntent(String customerId) {

        try {

            final var params = SetupIntentCreateParams.builder()
                    .setCustomer(customerId)
                    .addPaymentMethodType("card")
                    .build();

            final var intent = gateway.createSetupIntent(params);
            return new CreateSetupIntentResponse(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PaymentMethodSummary> listPaymentMethods(String customerId) {

        try {

            final var params = CustomerListPaymentMethodsParams.builder().build();

            return gateway.listPaymentMethods(customerId, params).getData().stream()
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
    public CreatePaymentIntentResponse createPaymentIntent(final CreatePaymentIntentRequest request) {

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

            final var intent = gateway.createPaymentIntent(builder.build(), request.idempotencyKey());
            return new CreatePaymentIntentResponse(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId) {

        try {

            final var sub = gateway.retrieveSubscription(subscriptionId);
            return toStatusResponse(sub);

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public SubscriptionListResponse listSubscriptionsByCustomer(String customerId, String status, int limit, String startingAfter) {

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

            final var collection = gateway.listSubscriptions(builder.build());

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

        try {

            final var builder = SessionCreateParams.builder().setCustomer(customerId);

            if (returnUrl != null && !returnUrl.isBlank()) {
                builder.setReturnUrl(returnUrl);
            }

            return gateway.createBillingPortalSession(builder.build()).getUrl();

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public SubscriptionStatusResponse createSubscription(String customerId, CreateSubscriptionRequest request) {

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

            final var sub = gateway.createSubscription(paramsBuilder.build(), request.idempotencyKey());
            return toStatusResponse(sub);

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public SubscriptionStatusResponse cancelSubscription(String subscriptionId) {

        try {
            return toStatusResponse(gateway.cancelSubscription(subscriptionId));
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<ProductSummary> listProducts(boolean activeOnly, int limit) {

        try {

            final var params = ProductListParams.builder()
                    .setActive(activeOnly)
                    .setLimit((long) limit)
                    .addExpand("data.default_price")
                    .build();

            return gateway.listProducts(params).getData().stream()
                    .map(p -> new ProductSummary(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            Boolean.TRUE.equals(p.getActive()),
                            p.getDefaultPriceObject() != null ? mapPrice(p.getDefaultPriceObject()) : null))
                    .toList();

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<PriceSummary> listPrices(String productId, boolean activeOnly, int limit) {

        final var cacheKey = productId + "|" + activeOnly + "|" + limit;
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

            final var prices = gateway.listPrices(builder.build()).getData().stream()
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

        try {
            return mapPrice(gateway.retrievePrice(priceId));
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<MeterSummary> listMeters(boolean activeOnly, int limit) {

        try {

            final var builder = MeterListParams.builder().setLimit((long) limit);

            if (activeOnly) {
                builder.setStatus(MeterListParams.Status.ACTIVE);
            }

            return gateway.listMeters(builder.build()).getData().stream()
                    .map(this::mapMeter)
                    .toList();

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public Optional<String> findCustomerByMetadata(String metadataKey, String metadataValue) {

        try {

            final var query = "metadata['" + metadataKey + "']:'" + metadataValue + "'";
            final var params = CustomerSearchParams.builder()
                    .setQuery(query)
                    .setLimit(1L)
                    .build();

            return gateway.searchCustomers(params).getData().stream()
                    .findFirst()
                    .map(Customer::getId);

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public CreateCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {

        try {

            final var mode = "payment".equalsIgnoreCase(request.mode())
                    ? com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT
                    : com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION;

            final var paramsBuilder = com.stripe.param.checkout.SessionCreateParams.builder()
                    .setCustomer(request.customerId())
                    .setMode(mode)
                    .setSuccessUrl(request.successUrl())
                    .setCancelUrl(request.cancelUrl())
                    .addLineItem(com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                            .setPrice(request.priceId())
                            .setQuantity(1L)
                            .build());

            if (request.metadata() != null && !request.metadata().isEmpty()) {
                request.metadata().forEach(paramsBuilder::putMetadata);
                if (mode == com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION) {
                    final var subData = com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder();
                    request.metadata().forEach(subData::putMetadata);
                    paramsBuilder.setSubscriptionData(subData.build());
                } else {
                    final var piData = com.stripe.param.checkout.SessionCreateParams.PaymentIntentData.builder();
                    request.metadata().forEach(piData::putMetadata);
                    paramsBuilder.setPaymentIntentData(piData.build());
                }
            }

            final var session = gateway.createCheckoutSession(paramsBuilder.build(), request.idempotencyKey());
            return new CreateCheckoutSessionResponse(session.getId(), session.getUrl());

        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public List<InvoiceSummary> listInvoices(String customerId, int limit, String startingAfter) {

        try {

            final var builder = InvoiceListParams.builder()
                    .setCustomer(customerId)
                    .setLimit((long) limit);

            if (startingAfter != null && !startingAfter.isBlank()) {
                builder.setStartingAfter(startingAfter);
            }

            return gateway.listInvoices(builder.build()).getData().stream()
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
    public void recordMeterEvent(String customerId, String eventName, long value, String idempotencyKey) {

        try {

            final var params = MeterEventCreateParams.builder()
                    .setEventName(eventName)
                    .setIdentifier(idempotencyKey)
                    .putPayload("stripe_customer_id", customerId)
                    .putPayload("value", String.valueOf(value))
                    .build();

            gateway.createMeterEvent(params, idempotencyKey);

        } catch (StripeException e) {
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

    private MeterSummary mapMeter(com.stripe.model.billing.Meter meter) {
        return new MeterSummary(
                meter.getId(),
                meter.getDisplayName(),
                meter.getEventName(),
                meter.getStatus());
    }

}
