package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionRequest;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionResponse;
import dev.getelements.elements.stripe.model.CreateCustomerResponse;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreatePortalSessionResponse;
import dev.getelements.elements.stripe.model.CreateSubscriptionRequest;
import dev.getelements.elements.stripe.model.InvoiceSummary;
import dev.getelements.elements.stripe.model.PriceSummary;
import dev.getelements.elements.stripe.model.ProductSummary;
import dev.getelements.elements.stripe.model.RecordMeterEventRequest;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import dev.getelements.elements.stripe.model.UpdateCustomerRequest;

import java.util.List;
import dev.getelements.elements.stripe.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static dev.getelements.elements.sdk.jakarta.rs.AuthSchemes.SESSION_SECRET;
import static dev.getelements.elements.stripe.StripeApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/stripe")
public class StripePaymentEndpoint {

    /**
     * Selects sandbox vs. production for this call. Omit to fall back to production if
     * configured, sandbox otherwise.
     */
    public static final String MODE_HEADER = "X-Stripe-Mode";

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
            description = "Creates a Stripe PaymentIntent and returns the client secret for front-end confirmation. " +
                    "Supply an idempotencyKey to safely retry without double-charging.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public CreatePaymentIntentResponse createPaymentIntent(
            CreatePaymentIntentRequest request,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.createPaymentIntent(request, mode)
                : stripeService.createPaymentIntent(request);
    }

    @PATCH
    @Path("/customer/{customerId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Update a customer",
            description = "Updates the email and/or display name of an existing Stripe customer. Null fields are left unchanged.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public Response updateCustomer(
            @PathParam("customerId") String customerId,
            UpdateCustomerRequest request,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        if (mode != null) {
            stripeService.updateCustomer(customerId, request.email(), request.name(), mode);
        } else {
            stripeService.updateCustomer(customerId, request.email(), request.name());
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/subscription/{subscriptionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get subscription status",
            description = "Retrieves the current status of a Stripe subscription.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public SubscriptionStatusResponse getSubscriptionStatus(
            @PathParam("subscriptionId") String subscriptionId,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.getSubscriptionStatus(subscriptionId, mode)
                : stripeService.getSubscriptionStatus(subscriptionId);
    }

    @DELETE
    @Path("/subscription/{subscriptionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Cancel a subscription",
            description = "Immediately cancels a Stripe subscription. The customer loses access at once. " +
                    "Returns the final subscription status with status = 'canceled'.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public SubscriptionStatusResponse cancelSubscription(
            @PathParam("subscriptionId") String subscriptionId,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.cancelSubscription(subscriptionId, mode)
                : stripeService.cancelSubscription(subscriptionId);
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
            @QueryParam("returnUrl") String returnUrl,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        final var url = mode != null
                ? stripeService.createBillingPortalSession(customerId, returnUrl, mode)
                : stripeService.createBillingPortalSession(customerId, returnUrl);
        return new CreatePortalSessionResponse(url);
    }

    @GET
    @Path("/products")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List Stripe products",
            description = "Returns products from the Stripe catalogue.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public List<ProductSummary> listProducts(
            @QueryParam("active") @DefaultValue("true") boolean activeOnly,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.listProducts(activeOnly, limit, mode)
                : stripeService.listProducts(activeOnly, limit);
    }

    @GET
    @Path("/prices")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List Stripe prices",
            description = "Returns prices from the Stripe product catalogue. Results are cached in memory " +
                    "(TTL controlled by dev.getelements.elements.stripe.price.cache.ttl.ms, default 5 minutes).",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public List<PriceSummary> listPrices(
            @QueryParam("productId") String productId,
            @QueryParam("active") @DefaultValue("true") boolean activeOnly,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.listPrices(productId, activeOnly, limit, mode)
                : stripeService.listPrices(productId, activeOnly, limit);
    }

    @GET
    @Path("/prices/{priceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Retrieve a single Stripe price",
            description = "Fetches a price directly by ID — useful when you have a price ID but not the product ID.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public PriceSummary retrievePrice(
            @PathParam("priceId") String priceId,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.retrievePrice(priceId, mode)
                : stripeService.retrievePrice(priceId);
    }

    @GET
    @Path("/customers/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Find a customer by metadata",
            description = "Searches for a Stripe customer by a metadata key-value pair. Returns 404 if no customer matches. " +
                    "Use key=orgId to implement find-or-create and avoid orphaned customers.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public CreateCustomerResponse findCustomerByMetadata(
            @QueryParam("key") String key,
            @QueryParam("value") String value,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        final var result = mode != null
                ? stripeService.findCustomerByMetadata(key, value, mode)
                : stripeService.findCustomerByMetadata(key, value);
        return result
                .map(CreateCustomerResponse::new)
                .orElseThrow(jakarta.ws.rs.NotFoundException::new);
    }

    @POST
    @Path("/customer/{customerId}/subscription")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a subscription",
            description = "Creates a recurring Stripe subscription for the customer. The customer must already have a " +
                    "default payment method on file. Supply an idempotencyKey to safely retry without creating duplicates.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public SubscriptionStatusResponse createSubscription(
            @PathParam("customerId") String customerId,
            CreateSubscriptionRequest request,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.createSubscription(customerId, request, mode)
                : stripeService.createSubscription(customerId, request);
    }

    @GET
    @Path("/customer/{customerId}/invoices")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List invoices for a customer",
            description = "Returns Stripe invoices for the given customer, newest first. Supports cursor pagination.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public List<InvoiceSummary> listInvoices(
            @PathParam("customerId") String customerId,
            @QueryParam("limit") @DefaultValue("10") int limit,
            @QueryParam("startingAfter") String startingAfter,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.listInvoices(customerId, limit, startingAfter, mode)
                : stripeService.listInvoices(customerId, limit, startingAfter);
    }

    @POST
    @Path("/checkout-session")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a Stripe-hosted Checkout Session",
            description = "Creates a Checkout Session and returns the hosted URL. Redirect the customer to this URL; " +
                    "Stripe handles payment method collection and confirmation, then redirects back to successUrl or cancelUrl. " +
                    "Supply an idempotencyKey to safely retry without creating a duplicate session.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public CreateCheckoutSessionResponse createCheckoutSession(
            CreateCheckoutSessionRequest request,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.createCheckoutSession(request, mode)
                : stripeService.createCheckoutSession(request);
    }

    @POST
    @Path("/meter-event")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Record a billing meter event",
            description = "Reports a usage event to Stripe's billing meter. The idempotencyKey is used as both the " +
                    "Stripe event identifier and the HTTP idempotency key, so retries are safe and will never double-charge.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public Response recordMeterEvent(
            RecordMeterEventRequest request,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        if (mode != null) {
            stripeService.recordMeterEvent(
                    request.customerId(),
                    request.eventName(),
                    request.value(),
                    request.idempotencyKey(),
                    mode);
        } else {
            stripeService.recordMeterEvent(
                    request.customerId(),
                    request.eventName(),
                    request.value(),
                    request.idempotencyKey());
        }
        return Response.noContent().build();
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
            @QueryParam("startingAfter") String startingAfter,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = parseMode(modeHeader);
        return mode != null
                ? stripeService.listSubscriptionsByCustomer(customerId, status, limit, startingAfter, mode)
                : stripeService.listSubscriptionsByCustomer(customerId, status, limit, startingAfter);
    }

    static StripeMode parseMode(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return StripeMode.valueOf(header.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + MODE_HEADER + " header: " + header);
        }
    }

}
