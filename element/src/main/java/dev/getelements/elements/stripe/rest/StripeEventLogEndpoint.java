package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.model.StripeEventLogResponse;
import dev.getelements.elements.stripe.service.StripeEventLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static dev.getelements.elements.sdk.jakarta.rs.AuthSchemes.SESSION_SECRET;
import static dev.getelements.elements.stripe.StripeApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/stripe/events")
public class StripeEventLogEndpoint {

    private final StripeEventLogService eventLogService;

    /** Used by the JAX-RS container at runtime. */
    public StripeEventLogEndpoint() {
        final var el = ElementSupplier.getElementLocal(StripeEventLogEndpoint.class).get();
        this.eventLogService = el.getServiceLocator().getInstance(StripeEventLogService.class);
    }

    /** Package-private — used by unit tests. */
    StripeEventLogEndpoint(StripeEventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List Stripe webhook events",
            description = "Returns received Stripe webhook events, newest first. Supports filtering by event type and offset pagination.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public StripeEventLogResponse listEvents(
            @QueryParam("type") String type,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        return eventLogService.listEvents(type, limit, offset);
    }

}
