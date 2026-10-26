package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeDualConfig;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.service.StripeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static dev.getelements.elements.sdk.jakarta.rs.AuthSchemes.SESSION_SECRET;
import static dev.getelements.elements.stripe.StripeApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/stripe/config")
@SecurityRequirement(name = SESSION_SECRET)
public class StripeConfigEndpoint {

    private final StripeConfigService configService;

    public StripeConfigEndpoint() {
        final var el = ElementSupplier.getElementLocal(StripeConfigEndpoint.class).get();
        this.configService = el.getServiceLocator().getInstance(StripeConfigService.class);
    }

    /** Package-private — used by unit tests to supply a mock service. */
    StripeConfigEndpoint(StripeConfigService configService) {
        this.configService = configService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get Stripe configuration",
            description = "Returns the current production and sandbox Stripe credentials with plaintext values. " +
                    "Confidentiality is the client's responsibility: the UI renders these fields as password " +
                    "inputs (masking and optionally revealing client-side)."
    )
    public StripeDualConfig getConfig() {
        return new StripeDualConfig(configService.getConfig(StripeMode.PRODUCTION), configService.getConfig(StripeMode.SANDBOX));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Save Stripe configuration",
            description = "Persists production and/or sandbox Stripe credentials to the database, overriding the " +
                    "Element's default attributes. Values are persisted as given (including empty string, which " +
                    "clears the field and restores the attribute default). Submissions containing the UI mask " +
                    "glyph are rejected so a stale client cannot shadow the real credential."
    )
    public Response saveConfig(StripeDualConfig config) {

        if (config.production() != null) {
            validateNotMasked(config.production());
        }

        if (config.sandbox() != null) {
            validateNotMasked(config.sandbox());
        }

        // Validation of both sections precedes any persistence so a rejection leaves the
        // stored configuration untouched (no half-applied submission).
        if (config.production() != null) {
            configService.saveConfig(config.production(), StripeMode.PRODUCTION);
        }

        if (config.sandbox() != null) {
            configService.saveConfig(config.sandbox(), StripeMode.SANDBOX);
        }

        return Response.ok("{\"saved\":true}").build();
    }

    /**
     * Rejects values still containing the UI mask glyph. The server never issues masks anymore, so a
     * submission shaped like one can only come from a stale client bundle that round-tripped previously
     * masked placeholders — persisting one would shadow the real credential (issue #12).
     */
    private void validateNotMasked(StripeConfig config) {
        if (config.apiKey() != null && config.apiKey().contains("•")) {
            throw new jakarta.ws.rs.BadRequestException("Masked API keys cannot be saved. Please re-enter the full values.");
        }
        if (config.webhookSecret() != null && config.webhookSecret().contains("•")) {
            throw new jakarta.ws.rs.BadRequestException("Masked webhook secrets cannot be saved. Please re-enter the full values.");
        }
    }

}
