package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
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
import dev.getelements.elements.stripe.model.CreateCustomerResponse;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreateSetupIntentResponse;
import dev.getelements.elements.stripe.model.PaymentMethodSummary;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.inject.Provider;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;

import java.time.Instant;
import java.util.List;

@ElementServiceExport(StripeService.class)
public class StripeServiceImpl implements StripeService {

    private static final String RECEIPT_SCHEMA = "stripe";

    private final StripeGateway gateway;
    private final UserService userService;
    private final Provider<Transaction> transactionProvider;

    @Inject
    public StripeServiceImpl(
            StripeGateway gateway,
            UserService userService,
            Provider<Transaction> transactionProvider) {
        this.gateway = gateway;
        this.userService = userService;
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

            final var customer = gateway.createCustomer(params);
            return new CreateCustomerResponse(customer.getId());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
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

            final var params = CustomerListPaymentMethodsParams.builder()
                    .setType(CustomerListPaymentMethodsParams.Type.CARD)
                    .build();

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

            final var params = PaymentIntentCreateParams.builder()
                    .setAmount(request.amount())
                    .setCurrency(request.currency())
                    .setCustomer(request.customerId())
                    .putMetadata(StripeService.METADATA_USER_ID, userService.getCurrentUser().getId())
                    .build();

            final var intent = gateway.createPaymentIntent(params);

            return new CreatePaymentIntentResponse(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public SubscriptionStatusResponse getSubscriptionStatus(String subscriptionId) {

        try {

            final var sub = gateway.retrieveSubscription(subscriptionId);
            final var periodEnd = sub.getCurrentPeriodEnd() != null
                    ? Instant.ofEpochSecond(sub.getCurrentPeriodEnd()).toString()
                    : null;

            return new SubscriptionStatusResponse(sub.getId(), sub.getStatus(), periodEnd);

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
                    .map(sub -> {
                        final var periodEnd = sub.getCurrentPeriodEnd() != null
                                ? Instant.ofEpochSecond(sub.getCurrentPeriodEnd()).toString()
                                : null;
                        return new SubscriptionStatusResponse(sub.getId(), sub.getStatus(), periodEnd);
                    })
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

        final var user = userService.getUser(userId);
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

}
