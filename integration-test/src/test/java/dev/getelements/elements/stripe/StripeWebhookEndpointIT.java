package dev.getelements.elements.stripe;

import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.stripe.rest.StripeWebhookEndpoint;
import dev.getelements.elements.stripe.service.StripeEventLogService;
import dev.getelements.elements.stripe.service.StripeService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class StripeWebhookEndpointIT {

    /**
     * Not a real Stripe secret - only used as the shared HMAC key between the
     * signature this test generates and the endpoint under test.
     */
    private static final String webhookSecret = "whsec_test_secret_for_unit_testing_only";

    private static String buildSignatureHeader(String payload, String secret, long timestamp) throws Exception {
        final var signedPayload = timestamp + "." + payload;
        final var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        final var hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        final var hex = new StringBuilder();
        for (var b : hash) hex.append(String.format("%02x", b));
        return "t=" + timestamp + ",v1=" + hex;
    }

    private StripeWebhookEndpoint endpoint() {
        return new StripeWebhookEndpoint(mock(Element.class), webhookSecret, mock(StripeService.class), mock(StripeEventLogService.class));
    }

    @Test
    void receiveWebhook_validSignature_returns200() throws Exception {
        final var payload = """
                {
                  "id": "evt_it_001",
                  "object": "event",
                  "api_version": "2024-04-10",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "pi_it_001",
                      "object": "payment_intent",
                      "amount": 1000,
                      "currency": "usd",
                      "status": "succeeded"
                    }
                  }
                }
                """;

        final var timestamp = System.currentTimeMillis() / 1000L;
        final var sigHeader = buildSignatureHeader(payload, webhookSecret, timestamp);

        final var response = endpoint().receiveWebhook(payload, sigHeader);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void receiveWebhook_invalidSignature_returns400() {
        final var payload = "{\"id\":\"evt_it_002\",\"object\":\"event\",\"type\":\"ping\",\"data\":{\"object\":{}}}";
        final var badSigHeader = "t=1234567890,v1=invalidsignature";

        final var response = endpoint().receiveWebhook(payload, badSigHeader);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void receiveWebhook_missingSecret_returns503() {
        final var endpoint = new StripeWebhookEndpoint(mock(Element.class), "", mock(StripeService.class), mock(StripeEventLogService.class));
        final var payload = "{\"id\":\"evt_it_003\",\"object\":\"event\",\"type\":\"ping\",\"data\":{\"object\":{}}}";

        final var response = endpoint.receiveWebhook(payload, "t=1234567890,v1=anything");

        assertEquals(503, response.getStatus());
    }
}
