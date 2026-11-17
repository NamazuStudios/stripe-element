package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecordResponse;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.service.MeterPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import static dev.getelements.elements.sdk.jakarta.rs.AuthSchemes.SESSION_SECRET;
import static dev.getelements.elements.stripe.StripeApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/stripe/postings")
public class StripePostingEndpoint {

    private final MeterPostingService postingService;

    /** Used by the JAX-RS container at runtime. */
    public StripePostingEndpoint() {
        final var element = ElementSupplier.getElementLocal(StripePostingEndpoint.class).get();
        this.postingService = element.getServiceLocator().getInstance(MeterPostingService.class);
    }

    /** Package-private — used by unit tests. */
    StripePostingEndpoint(MeterPostingService postingService) {
        this.postingService = postingService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List posting records",
            description = "Returns the posting-record store — what each billing meter-event posting attempt actually " +
                    "did (posted / skipped / failed), newest first. Supports identifier-prefix, customer, event, outcome, " +
                    "and mode filtering with offset pagination. The mode filter defaults to both.",
            security = {@SecurityRequirement(name = SESSION_SECRET)}
    )
    public PostingRecordResponse listPostings(
            @Parameter(description = "Identifier prefix — the idempotency key convention 'batchId:orgId:meterName' makes batch- and org-scoped queries natural")
            @QueryParam("identifier") String identifierPrefix,
            @QueryParam("customer") String customerId,
            @QueryParam("event") String eventName,
            @QueryParam("outcome") String outcome,
            @Parameter(description = "sandbox, production, or omit/both for both")
            @QueryParam("mode") String mode,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        return postingService.listPostings(
                identifierPrefix,
                customerId,
                eventName,
                parseOutcome(outcome),
                parseModeFilter(mode),
                limit,
                offset);
    }

    private static PostingOutcome parseOutcome(String outcome) {
        if (outcome == null || outcome.isBlank()) {
            return null;
        }
        try {
            return PostingOutcome.valueOf(outcome.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid outcome: " + outcome);
        }
    }

    /**
     * The admin-facing tri-state: a real mode name restricts to it; {@code both} (or omitted)
     * matches records from either mode.
     */
    private static StripeMode parseModeFilter(String mode) {
        if (mode == null || mode.isBlank() || "both".equalsIgnoreCase(mode.trim())) {
            return null;
        }
        try {
            return StripeMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid mode: " + mode + " (expected sandbox, production, or both)");
        }
    }

}
