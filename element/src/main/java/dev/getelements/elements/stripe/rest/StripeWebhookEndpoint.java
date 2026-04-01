package dev.getelements.elements.stripe.rest;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.sdk.annotation.ElementEventProducer;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.StripeEvents;
import dev.getelements.elements.stripe.event.StripePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCancelledEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCreatedEvent;
import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.service.StripeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static dev.getelements.elements.stripe.StripeApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/stripe/webhook")
@ElementEventProducer(
        value = StripeEvents.PAYMENT_SUCCEEDED,
        description = "Published when a Stripe payment_intent.succeeded webhook is received.")
@ElementEventProducer(
        value = StripeEvents.PAYMENT_FAILED,
        description = "Published when a Stripe payment_intent.payment_failed webhook is received.")
@ElementEventProducer(
        value = StripeEvents.SUBSCRIPTION_CREATED,
        description = "Published when a Stripe customer.subscription.created webhook is received.")
@ElementEventProducer(
        value = StripeEvents.SUBSCRIPTION_CANCELLED,
        description = "Published when a Stripe customer.subscription.deleted webhook is received.")
public class StripeWebhookEndpoint {

    private final Element element;
    private final StripeConfigService configService;

    /** Used by the JAX-RS container at runtime. */
    public StripeWebhookEndpoint() {
        final var el = ElementSupplier.getElementLocal(StripeWebhookEndpoint.class).get();
        this.element = el;
        this.configService = el.getServiceLocator().getInstance(StripeConfigService.class);
    }

    /**
     * Package-private — used by unit tests to supply a mock Element and a fixed secret
     * without going through the service locator.
     */
    StripeWebhookEndpoint(Element element, String webhookSecret) {
        this.element = element;
        this.configService = new StripeConfigService() {
            @Override public StripeConfig getConfig() { return new StripeConfig("", webhookSecret); }
            @Override public void saveConfig(StripeConfig c) {}
        };
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Stripe webhook receiver",
            description = "Receives and verifies Stripe webhook events, then publishes typed internal events."
    )
    public Response receiveWebhook(
            String payload,
            @HeaderParam("Stripe-Signature") String sigHeader) {

        final var secret = configService.getConfig().webhookSecret();

        if (secret == null || secret.isBlank()) {
            return Response.status(503)
                    .entity("{\"error\":\"Webhook secret not configured\"}")
                    .build();
        }

        final Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, secret);
        } catch (SignatureVerificationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid signature\"}")
                    .build();
        }

        switch (event.getType()) {

            case StripeEvents.PAYMENT_SUCCEEDED -> {

                final var pi = (com.stripe.model.PaymentIntent)
                        event.getDataObjectDeserializer().getObject().orElseThrow();

                element.publish(new StripePaymentSucceededEvent(
                        pi.getId(),
                        pi.getAmount(),
                        pi.getCurrency()
                ));
            }

            case StripeEvents.PAYMENT_FAILED -> {

                final var pi = (com.stripe.model.PaymentIntent)
                        event.getDataObjectDeserializer().getObject().orElseThrow();

                final var failureMessage = pi.getLastPaymentError() != null
                        ? pi.getLastPaymentError().getMessage()
                        : "Unknown failure";

                element.publish(new StripePaymentFailedEvent(pi.getId(), failureMessage));
            }

            case StripeEvents.SUBSCRIPTION_CREATED -> {

                final var sub = (Subscription)
                        event.getDataObjectDeserializer().getObject().orElseThrow();

                element.publish(new StripeSubscriptionCreatedEvent(
                        sub.getId(),
                        sub.getCustomer(),
                        sub.getStatus()
                ));
            }

            case StripeEvents.SUBSCRIPTION_CANCELLED -> {

                final var sub = (Subscription)
                        event.getDataObjectDeserializer().getObject().orElseThrow();

                element.publish(new StripeSubscriptionCancelledEvent(
                        sub.getId(),
                        sub.getCustomer()
                ));
            }

            default -> { /* unhandled event type - ignore */ }
        }

        return Response.ok("{\"received\":true}").build();
    }

}
