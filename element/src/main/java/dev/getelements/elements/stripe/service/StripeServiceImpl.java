package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
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
                    .build();

            final var intent = gateway.createPaymentIntent(params);

            saveReceipt(intent.getId(), request);

            return new CreatePaymentIntentResponse(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            throw new InternalServerErrorException("Stripe error: " + e.getMessage(), e);
        }
    }

    private void saveReceipt(String paymentIntentId, CreatePaymentIntentRequest request) {
        final var receipt = new Receipt();
        receipt.setOriginalTransactionId(paymentIntentId);
        receipt.setSchema(RECEIPT_SCHEMA);
        receipt.setUser(userService.getCurrentUser());
        receipt.setPurchaseTime(System.currentTimeMillis());
        receipt.setBody(String.format(
                "{\"amount\":%d,\"currency\":\"%s\",\"customerId\":\"%s\"}",
                request.amount(), request.currency(), request.customerId()));

        transactionProvider.get().performAndCloseV(txn ->
                txn.getDao(ReceiptDao.class).createReceipt(receipt));
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

}
