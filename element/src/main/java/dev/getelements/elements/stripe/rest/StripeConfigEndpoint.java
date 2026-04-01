package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.model.StripeConfig;
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get Stripe configuration",
            description = "Returns the current Stripe credentials with values masked for display."
    )
    public StripeConfig getConfig() {
        final var config = configService.getConfig();
        return new StripeConfig(mask(config.apiKey()), mask(config.webhookSecret()));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Save Stripe configuration",
            description = "Persists Stripe credentials to the database, overriding the Element's default attributes."
    )
    public Response saveConfig(StripeConfig config) {
        configService.saveConfig(config);
        return Response.ok("{\"saved\":true}").build();
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() <= 8) return "••••••••";
        return "••••" + value.substring(value.length() - 4);
    }

}
