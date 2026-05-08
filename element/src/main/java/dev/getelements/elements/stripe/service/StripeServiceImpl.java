package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.dao.ReceiptDao;
import dev.getelements.elements.sdk.dao.Transaction;
import dev.getelements.elements.sdk.model.receipt.Receipt;
import dev.getelements.elements.sdk.service.user.UserService;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.inject.Provider;
import jakarta.ws.rs.InternalServerErrorException;

import java.time.Instant;

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
