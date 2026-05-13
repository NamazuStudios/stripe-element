package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreatePortalSessionResponse;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import dev.getelements.elements.stripe.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static dev.getelements.elements.sdk.jakarta.rs.AuthSchemes.SESSION_SECRET;
import static dev.getelements.elements.stripe.StripeApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/stripe")
public class StripePaymentEndpoint {

    private final StripeService stripeService;

    /** Used by the JAX-RS container at runtime. */
    public StripePaymentEndpoint() {
        final var element = ElementSupplier.getElementLocal(StripePaymentEndpoint.class).get();
        this.stripeService = element.getServiceLocator().getInstance(StripeService.class);
    }

    /** Package-private — used by unit tests to supply a mock service. */
    StripePaymentEndpoint(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @POST
    @Path("/payment-intent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a PaymentIntent",
            description = "Creates a Stripe PaymentIntent and returns the client secret for front-end confirmation.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        return stripeService.createPaymentIntent(request);
    }

    @GET
    @Path("/subscription/{subscriptionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get subscription status",
            description = "Retrieves the current status of a Stripe subscription.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public SubscriptionStatusResponse getSubscriptionStatus(@PathParam("subscriptionId") String subscriptionId) {
        return stripeService.getSubscriptionStatus(subscriptionId);
    }

    @POST
    @Path("/customer/{customerId}/portal-session")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a Customer Portal session",
            description = "Creates a Stripe Customer Portal session URL. The returned URL is single-use and expires after a short period.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public CreatePortalSessionResponse createPortalSession(
            @PathParam("customerId") String customerId,
            @QueryParam("returnUrl") String returnUrl) {
        return new CreatePortalSessionResponse(stripeService.createBillingPortalSession(customerId, returnUrl));
    }

    @GET
    @Path("/customer/{customerId}/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List subscriptions for a customer",
            description = "Returns Stripe subscriptions for the given customer. Supports status filtering and cursor pagination.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public SubscriptionListResponse listSubscriptionsByCustomer(
            @PathParam("customerId") String customerId,
            @QueryParam("status") String status,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("startingAfter") String startingAfter) {
        return stripeService.listSubscriptionsByCustomer(customerId, status, limit, startingAfter);
    }

}
