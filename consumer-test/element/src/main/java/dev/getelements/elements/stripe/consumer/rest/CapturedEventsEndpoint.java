package dev.getelements.elements.stripe.consumer.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.stripe.consumer.service.StripeEventCapture;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Exposes the in-memory event capture store over HTTP for integration test assertions.
 *
 * <p>Base path: {@code /element/stripe/consumer/captured-events}
 */
@Path("/captured-events")
public class CapturedEventsEndpoint {

    private final StripeEventCapture capture;

    public CapturedEventsEndpoint() {
        final var el = ElementSupplier.getElementLocal(CapturedEventsEndpoint.class).get();
        this.capture = el.getServiceLocator().getInstance(StripeEventCapture.class);
    }

    @GET
    @Path("/payment-succeeded")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> paymentSucceeded() {
        return capture.paymentSucceededIds();
    }

    @GET
    @Path("/payment-failed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> paymentFailed() {
        return capture.paymentFailedIds();
    }

    @GET
    @Path("/subscription-created")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> subscriptionCreated() {
        return capture.subscriptionCreatedIds();
    }

    @GET
    @Path("/subscription-cancelled")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> subscriptionCancelled() {
        return capture.subscriptionCancelledIds();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response clear() {
        capture.clear();
        return Response.ok("{\"cleared\":true}").build();
    }

}
