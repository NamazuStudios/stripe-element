package dev.getelements.elements.stripe.service;

import com.google.inject.Inject;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.ws.rs.InternalServerErrorException;

import java.time.Instant;

@ElementServiceExport(StripeService.class)
public class StripeServiceImpl implements StripeService {

    private final StripeGateway gateway;

    @Inject
    public StripeServiceImpl(StripeGateway gateway) {
        this.gateway = gateway;
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

}
