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
            description = "Returns the current production and sandbox Stripe credentials, values masked for display."
    )
    public StripeDualConfig getConfig() {
        return new StripeDualConfig(maskedConfig(StripeMode.PRODUCTION), maskedConfig(StripeMode.SANDBOX));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Save Stripe configuration",
            description = "Persists production and/or sandbox Stripe credentials to the database, overriding the " +
                    "Element's default attributes. A field whose value still matches the masked placeholder " +
                    "previously returned by GET is left unchanged; any other value (including empty string, " +
                    "which clears the field) is persisted as given."
    )
    public Response saveConfig(StripeDualConfig config) {

        if (config.production() != null) {
            configService.saveConfig(resolveEdits(config.production(), StripeMode.PRODUCTION), StripeMode.PRODUCTION);
        }

        if (config.sandbox() != null) {
            configService.saveConfig(resolveEdits(config.sandbox(), StripeMode.SANDBOX), StripeMode.SANDBOX);
        }

        return Response.ok("{\"saved\":true}").build();
    }

    private StripeConfig maskedConfig(StripeMode mode) {
        final var config = configService.getConfig(mode);
        return new StripeConfig(mask(config.apiKey()), mask(config.webhookSecret()));
    }

    /**
     * A submitted field equal to the mask of the currently *stored* value (not the
     * attribute-resolved value returned by {@link StripeConfigService#getConfig}) is treated as
     * "unchanged" and left alone; any other value, including blank, is a real edit.
     */
    private StripeConfig resolveEdits(StripeConfig submitted, StripeMode mode) {
        final var raw = configService.getRawConfig(mode);
        final var apiKey = mask(raw.apiKey()).equals(submitted.apiKey()) ? raw.apiKey() : submitted.apiKey();
        final var secret = mask(raw.webhookSecret()).equals(submitted.webhookSecret()) ? raw.webhookSecret() : submitted.webhookSecret();
        return new StripeConfig(apiKey, secret);
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() <= 8) return "••••••••";
        return "••••" + value.substring(value.length() - 4);
    }

}
