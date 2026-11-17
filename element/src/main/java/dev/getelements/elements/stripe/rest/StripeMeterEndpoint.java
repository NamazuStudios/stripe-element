package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.model.MeterSummary;
import dev.getelements.elements.stripe.model.MeterUsageSummary;
import dev.getelements.elements.stripe.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static dev.getelements.elements.sdk.jakarta.rs.AuthSchemes.SESSION_SECRET;
import static dev.getelements.elements.stripe.StripeApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/stripe/meters")
public class StripeMeterEndpoint {

    /**
     * Selects sandbox vs. production for this call. Omit to fall back to production if
     * configured, sandbox otherwise.
     */
    public static final String MODE_HEADER = "X-Stripe-Mode";

    private final StripeService stripeService;

    /** Used by the JAX-RS container at runtime. */
    public StripeMeterEndpoint() {
        final var element = ElementSupplier.getElementLocal(StripeMeterEndpoint.class).get();
        this.stripeService = element.getServiceLocator().getInstance(StripeService.class);
    }

    /** Package-private — used by unit tests to supply a mock service. */
    StripeMeterEndpoint(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List billing meters",
            description = "Returns the Stripe Billing Meters configured on this account. A meter's eventName is the " +
                    "value to pass when recording usage via the meter-event endpoint.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public List<MeterSummary> listMeters(
            @QueryParam("active") @DefaultValue("true") boolean activeOnly,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = StripePaymentEndpoint.parseMode(modeHeader);
        return mode != null
                ? stripeService.listMeters(activeOnly, limit, mode)
                : stripeService.listMeters(activeOnly, limit);
    }

    @GET
    @Path("/usage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List aggregated meter usage for a customer",
            description = "Returns Stripe's aggregated usage for one meter (by event name) and customer over a time " +
                    "range — the read-side counterpart to recording meter events. Stripe exposes no listing of raw " +
                    "meter events, so verification sums these aggregates against the caller's own posting ledger. " +
                    "Timestamps are ISO-8601 (e.g. 2026-11-15T00:00:00Z); both default to the trailing 24 hours.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public List<MeterUsageSummary> listMeterUsage(
            @Parameter(description = "Meter event name, as passed when recording usage", required = true)
            @QueryParam("event") String event,
            @Parameter(description = "Stripe customer ID (cus_...)", required = true)
            @QueryParam("customer") String customer,
            @Parameter(description = "Window start (inclusive, ISO-8601); default: end minus 24 hours")
            @QueryParam("start") String start,
            @Parameter(description = "Window end (exclusive, ISO-8601); default: now")
            @QueryParam("end") String end,
            @Parameter(description = "\"hour\" to bucket results by hour; omit for a single aggregate")
            @QueryParam("window") String window,
            @Parameter(description = "sandbox or production") @HeaderParam(MODE_HEADER) String modeHeader) {
        final var mode = StripePaymentEndpoint.parseMode(modeHeader);
        final var startTime = parseInstant(start, "start");
        final var endTime = parseInstant(end, "end");
        return mode != null
                ? stripeService.listMeterUsage(event, customer, startTime, endTime, window, mode)
                : stripeService.listMeterUsage(event, customer, startTime, endTime, window);
    }

    private static Instant parseInstant(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(name + " must be ISO-8601 (e.g. 2026-11-15T00:00:00Z)");
        }
    }

}
