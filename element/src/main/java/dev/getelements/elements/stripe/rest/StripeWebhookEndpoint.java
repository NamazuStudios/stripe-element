package dev.getelements.elements.stripe.rest;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.sdk.annotation.ElementEventProducer;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.StripeEvents;
import dev.getelements.elements.stripe.event.StripeInvoicePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripeInvoicePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripeCheckoutSessionCompletedEvent;
import dev.getelements.elements.stripe.event.StripeRawEvent;
import dev.getelements.elements.stripe.event.StripePaymentCanceledEvent;
import dev.getelements.elements.stripe.event.StripePaymentMethodAttachedEvent;
import dev.getelements.elements.stripe.event.StripeSetupIntentSucceededEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCancelledEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCreatedEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionTrialWillEndEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionUpdatedEvent;
import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.service.StripeConfigService;
import dev.getelements.elements.stripe.service.StripeEventLogService;
import dev.getelements.elements.stripe.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

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
        value = StripeEvents.PAYMENT_CANCELED,
        description = "Published when a Stripe payment_intent.canceled webhook is received.")
@ElementEventProducer(
        value = StripeEvents.INVOICE_PAYMENT_SUCCEEDED,
        description = "Published when a Stripe invoice.payment_succeeded webhook is received.")
@ElementEventProducer(
        value = StripeEvents.INVOICE_PAYMENT_FAILED,
        description = "Published when a Stripe invoice.payment_failed webhook is received.")
@ElementEventProducer(
        value = StripeEvents.SUBSCRIPTION_CREATED,
        description = "Published when a Stripe customer.subscription.created webhook is received.")
@ElementEventProducer(
        value = StripeEvents.SUBSCRIPTION_UPDATED,
        description = "Published when a Stripe customer.subscription.updated webhook is received.")
@ElementEventProducer(
        value = StripeEvents.SUBSCRIPTION_CANCELLED,
        description = "Published when a Stripe customer.subscription.deleted webhook is received.")
@ElementEventProducer(
        value = StripeEvents.SUBSCRIPTION_TRIAL_WILL_END,
        description = "Published when a Stripe customer.subscription.trial_will_end webhook is received.")
@ElementEventProducer(
        value = StripeEvents.SETUP_INTENT_SUCCEEDED,
        description = "Published when a Stripe setup_intent.succeeded webhook is received.")
@ElementEventProducer(
        value = StripeEvents.PAYMENT_METHOD_ATTACHED,
        description = "Published when a Stripe payment_method.attached webhook is received.")
@ElementEventProducer(
        value = StripeEvents.CHECKOUT_SESSION_COMPLETED,
        description = "Published when a Stripe checkout.session.completed webhook is received.")
@ElementEventProducer(
        value = StripeEvents.RAW_WEBHOOK,
        description = "Published for every verified Stripe webhook, regardless of type.")
public class StripeWebhookEndpoint {

    private final Element element;
    private final StripeConfigService configService;
    private final StripeService stripeService;
    private final StripeEventLogService eventLogService;

    /** Used by the JAX-RS container at runtime. */
    public StripeWebhookEndpoint() {
        final var el = ElementSupplier.getElementLocal(StripeWebhookEndpoint.class).get();
        this.element = el;
        final var locator = el.getServiceLocator();
        this.configService = locator.getInstance(StripeConfigService.class);
        this.stripeService = locator.getInstance(StripeService.class);
        this.eventLogService = locator.getInstance(StripeEventLogService.class);
    }

    /**
     * Package-private — used by unit tests to supply dependencies without the service locator.
     */
    public StripeWebhookEndpoint(Element element, String webhookSecret, StripeService stripeService, StripeEventLogService eventLogService) {
        this.element = element;
        this.configService = new StripeConfigService() {
            @Override public StripeConfig getConfig() { return new StripeConfig("", webhookSecret); }
            @Override public void saveConfig(StripeConfig c) {}
        };
        this.stripeService = stripeService;
        this.eventLogService = eventLogService;
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

        // Always publish the raw event and log it, regardless of type.
        element.publish(new StripeRawEvent(event.getType(), event.getId(), payload));
        eventLogService.logEvent(event.getId(), event.getType());

        try {
            dispatchTypedEvent(event);
        } catch (EventDataObjectDeserializationException e) {
            // API version mismatch between this SDK and the incoming event — raw event was
            // already published above so consumers relying on RAW_WEBHOOK still fire.
            return Response.ok("{\"received\":true,\"typed\":false}").build();
        }

        return Response.ok("{\"received\":true}").build();
    }

    private void dispatchTypedEvent(Event event) throws EventDataObjectDeserializationException {

        switch (event.getType()) {

            case StripeEvents.PAYMENT_SUCCEEDED -> {

                final var pi = (PaymentIntent) event.getDataObjectDeserializer().deserializeUnsafe();

                element.publish(new StripePaymentSucceededEvent(
                        pi.getId(),
                        pi.getAmount(),
                        pi.getCurrency(),
                        pi.getCustomer()
                ));

                final var piMeta = pi.getMetadata();
                stripeService.recordPaymentReceipt(pi.getId(), pi.getAmount(), pi.getCurrency(),
                        piMeta != null ? piMeta.get(StripeService.METADATA_USER_ID) : null);
            }

            case StripeEvents.PAYMENT_FAILED -> {

                final var pi = (PaymentIntent) event.getDataObjectDeserializer().deserializeUnsafe();

                final var failureMessage = pi.getLastPaymentError() != null
                        ? pi.getLastPaymentError().getMessage()
                        : "Unknown failure";

                element.publish(new StripePaymentFailedEvent(pi.getId(), failureMessage, pi.getCustomer()));
            }

            case StripeEvents.PAYMENT_CANCELED -> {

                final var pi = (PaymentIntent) event.getDataObjectDeserializer().deserializeUnsafe();

                element.publish(new StripePaymentCanceledEvent(pi.getId(), pi.getCustomer()));
            }

            case StripeEvents.INVOICE_PAYMENT_SUCCEEDED -> {

                final var invoice = (Invoice) event.getDataObjectDeserializer().deserializeUnsafe();

                element.publish(new StripeInvoicePaymentSucceededEvent(
                        invoice.getId(),
                        invoice.getPaymentIntent(),
                        invoice.getAmountPaid(),
                        invoice.getCurrency()
                ));

                final var invMeta = invoice.getMetadata();
                stripeService.recordPaymentReceipt(invoice.getPaymentIntent(), invoice.getAmountPaid(),
                        invoice.getCurrency(), invMeta != null ? invMeta.get(StripeService.METADATA_USER_ID) : null);
            }

            case StripeEvents.INVOICE_PAYMENT_FAILED -> {

                final var invoice = (Invoice) event.getDataObjectDeserializer().deserializeUnsafe();

                final var failureMessage = invoice.getLastFinalizationError() != null
                        ? invoice.getLastFinalizationError().getMessage()
                        : "Payment failed";

                element.publish(new StripeInvoicePaymentFailedEvent(
                        invoice.getId(),
                        invoice.getSubscription(),
                        invoice.getCustomer(),
                        failureMessage
                ));
            }

            case StripeEvents.SUBSCRIPTION_CREATED -> {

                final var sub = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();

                final var meta = sub.getMetadata();
                element.publish(new StripeSubscriptionCreatedEvent(
                        sub.getId(),
                        sub.getCustomer(),
                        sub.getStatus(),
                        meta != null ? meta.get(StripeService.METADATA_ORG_ID) : null
                ));
            }

            case StripeEvents.SUBSCRIPTION_UPDATED -> {

                final var sub = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();

                final var meta = sub.getMetadata();
                element.publish(new StripeSubscriptionUpdatedEvent(
                        sub.getId(),
                        sub.getCustomer(),
                        sub.getStatus(),
                        meta != null ? meta.get(StripeService.METADATA_ORG_ID) : null
                ));
            }

            case StripeEvents.SUBSCRIPTION_CANCELLED -> {

                final var sub = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();

                final var meta = sub.getMetadata();
                element.publish(new StripeSubscriptionCancelledEvent(
                        sub.getId(),
                        sub.getCustomer(),
                        meta != null ? meta.get(StripeService.METADATA_ORG_ID) : null
                ));
            }

            case StripeEvents.SUBSCRIPTION_TRIAL_WILL_END -> {

                final var sub = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();

                final var meta = sub.getMetadata();
                final var trialEnd = sub.getTrialEnd() != null
                        ? Instant.ofEpochSecond(sub.getTrialEnd()).toString()
                        : null;

                element.publish(new StripeSubscriptionTrialWillEndEvent(
                        sub.getId(),
                        sub.getCustomer(),
                        trialEnd,
                        meta != null ? meta.get(StripeService.METADATA_ORG_ID) : null
                ));
            }

            case StripeEvents.SETUP_INTENT_SUCCEEDED -> {

                final var si = (SetupIntent) event.getDataObjectDeserializer().deserializeUnsafe();

                element.publish(new StripeSetupIntentSucceededEvent(si.getId(), si.getCustomer()));
            }

            case StripeEvents.PAYMENT_METHOD_ATTACHED -> {

                final var pm = (PaymentMethod) event.getDataObjectDeserializer().deserializeUnsafe();

                element.publish(new StripePaymentMethodAttachedEvent(pm.getId(), pm.getCustomer()));
            }

            case StripeEvents.CHECKOUT_SESSION_COMPLETED -> {

                final var session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();

                element.publish(new StripeCheckoutSessionCompletedEvent(
                        session.getId(),
                        session.getCustomer(),
                        session.getPaymentIntent(),
                        session.getSubscription(),
                        session.getMode(),
                        session.getMetadata()
                ));
            }

            default -> { /* unhandled event type — raw event already published above */ }
        }
    }

}
