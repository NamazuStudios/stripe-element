package dev.getelements.elements.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class StripeWebhookSignatureIT {

    private static final String TEST_SECRET = "whsec_test_secret_for_unit_testing_only";

    private String buildSignatureHeader(String payload, String secret, long timestamp) throws Exception {

        final var signedPayload = timestamp + "." + payload;
        final var mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        final var hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        final var hex = new StringBuilder();

        for (var b : hash) {
            hex.append(String.format("%02x", b));
        }

        return "t=" + timestamp + ",v1=" + hex;
    }

    @Test
    void verifySignature_valid_returnsEvent() throws Exception {

        final String payload = """
                {
                  "id": "evt_test_001",
                  "object": "event",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "pi_test_001",
                      "object": "payment_intent",
                      "amount": 1000,
                      "currency": "usd",
                      "status": "succeeded"
                    }
                  }
                }
                """;

        final var timestamp = System.currentTimeMillis() / 1000L;
        final var sigHeader = buildSignatureHeader(payload, TEST_SECRET, timestamp);
        final var event = Webhook.constructEvent(payload, sigHeader, TEST_SECRET);

        assertEquals("payment_intent.succeeded", event.getType());
    }

    @Test
    void verifySignature_invalid_throws() {

        final var payload = "{\"id\":\"evt_test\",\"object\":\"event\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{}}}";
        final var badSigHeader = "t=1234567890,v1=invalidsignature";

        assertThrows(SignatureVerificationException.class,
                () -> Webhook.constructEvent(payload, badSigHeader, TEST_SECRET));
    }

}
