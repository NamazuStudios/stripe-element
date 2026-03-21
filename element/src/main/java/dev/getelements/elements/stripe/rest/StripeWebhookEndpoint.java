package dev.getelements.elements.stripe.rest;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.event.StripePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCancelledEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCreatedEvent;
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
public class StripeWebhookEndpoint {

    private final Element element;

    /** Used by the JAX-RS container at runtime. */
    public StripeWebhookEndpoint() {
        this(ElementSupplier.getElementLocal(StripeWebhookEndpoint.class).get());
    }

    /** Package-private — used by unit tests to supply a mock Element. */
    StripeWebhookEndpoint(Element element) {
        this.element = element;
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

        final var secret = (String) element.getElementRecord().attributes()
                .getAttribute(StripeApplication.STRIPE_WEBHOOK_SECRET);

        final Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, secret);
        } catch (SignatureVerificationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid signature\"}")
                    .build();
        }

        switch (event.getType()) {

            case StripePaymentSucceededEvent.NAME -> {

                final var pi = (com.stripe.model.PaymentIntent)
                        event.getDataObjectDeserializer().getObject().orElseThrow();

                element.publish(new StripePaymentSucceededEvent(
                        pi.getId(),
                        pi.getAmount(),
                        pi.getCurrency()
                ));
            }

            case StripePaymentFailedEvent.NAME -> {

                final var pi = (com.stripe.model.PaymentIntent)
                        event.getDataObjectDeserializer().getObject().orElseThrow();

                final var failureMessage = pi.getLastPaymentError() != null
                        ? pi.getLastPaymentError().getMessage()
                        : "Unknown failure";

                element.publish(new StripePaymentFailedEvent(pi.getId(), failureMessage));
            }

            case StripeSubscriptionCreatedEvent.NAME -> {

                final var sub = (Subscription)
                        event.getDataObjectDeserializer().getObject().orElseThrow();

                element.publish(new StripeSubscriptionCreatedEvent(
                        sub.getId(),
                        sub.getCustomer(),
                        sub.getStatus()
                ));
            }

            case StripeSubscriptionCancelledEvent.NAME -> {

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
