package dev.getelements.elements.stripe.consumer;

import dev.getelements.elements.sdk.local.ElementsLocalBuilder;
import dev.getelements.elements.stripe.StripeApplication;
import dev.getelements.elements.stripe.model.WebhookPaymentIntentData;
import dev.getelements.elements.stripe.model.WebhookSubscriptionData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test that boots both the Stripe Element and the consumer Element
 * inside a local Elements runtime, fires signed webhook HTTP requests, and verifies that
 * the consumer Element received the published events.
 *
 * <p><b>Prerequisites:</b> both {@code dev.getelements.elements.stripe:element:elm} and
 * {@code dev.getelements.elements.stripe.consumer.test:element:elm} must be installed in
 * the local Maven repository before running this test:
 * <pre>
 *   cd ../..   &amp;&amp; mvn install          # installs the Stripe Element
 *   cd consumer-test &amp;&amp; mvn install    # installs the consumer Element
 *   mvn verify -pl integration-test   # runs these tests
 * </pre>
 */
class ConsumerElementIT {

    /** Signing secret injected into the Stripe Element via attribute override. */
    private static final String WEBHOOK_SECRET = "whsec_consumer_test_secret_only";

    private static final String STRIPE_BASE   = "http://localhost:8080/element/stripe/api";
    private static final String CONSUMER_BASE = "http://localhost:8080/element/stripe/consumer";

    private static AutoCloseable runtime;
    private static final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void startRuntime() throws Exception {
        /*
         * Deploy both Elements in a single local runtime.
         *
         * Attribute overrides are set inline on each elementPackage() builder — this is the
         * primary behaviour under test: verify that the Stripe Element actually uses the
         * secret we supply here rather than its compiled-in default.
         *
         * NOTE: the `.attribute(key, value)` call on the element-package builder is an
         * assumption about the sdk-local API. Adjust the method name if your version of
         * sdk-local differs (e.g. `.withAttribute(...)` or `.setAttribute(...)`).
         */
        final var local = ElementsLocalBuilder.getDefault()
                .withDeployment(builder -> builder
                        .useDefaultRepositories(true)
                        .elementPackage()
                            .elmArtifact("dev.getelements.elements.stripe:element:elm:1.0-SNAPSHOT")
                            .attribute(StripeApplication.STRIPE_WEBHOOK_SECRET, WEBHOOK_SECRET)
                            .attribute(StripeApplication.STRIPE_API_KEY, "sk_test_placeholder")
                            .attribute(StripeApplication.AUTH_ENABLED, "false")
                        .endElementPackage()
                        .elementPackage()
                            .elmArtifact("dev.getelements.elements.stripe.consumer.test:element:elm:1.0-SNAPSHOT")
                        .endElementPackage()
                        .build()
                )
                .build();

        local.start();
        runtime = local;

        // Allow both Elements time to finish initialising before the first test runs.
        Thread.sleep(8_000);
    }

    @AfterAll
    static void stopRuntime() throws Exception {
        if (runtime != null) runtime.close();
    }

    // -------------------------------------------------------------------------
    // Attribute override tests
    // -------------------------------------------------------------------------

    @Test
    void webhookSecret_override_validSignatureAccepted() throws Exception {
        final var data = new WebhookPaymentIntentData("pi_attr_test", 500, "usd", "succeeded", null);
        final var req  = StripeWebhookFixture.paymentSucceeded(data, WEBHOOK_SECRET);

        assertEquals(200, postWebhook(req).statusCode(),
                "Webhook signed with the overridden secret should be accepted");
    }

    @Test
    void webhookSecret_override_wrongSignatureRejected() throws Exception {
        final var data = new WebhookPaymentIntentData("pi_wrong_sig", 500, "usd", "succeeded", null);
        final var req  = StripeWebhookFixture.paymentSucceeded(data, "whsec_wrong_secret");

        assertEquals(400, postWebhook(req).statusCode(),
                "Webhook signed with a different secret should be rejected with 400");
    }

    // -------------------------------------------------------------------------
    // Cross-Element event delivery tests
    // -------------------------------------------------------------------------

    @Test
    void paymentSucceeded_webhook_isReceivedByConsumer() throws Exception {
        clearCapture();
        final var data = new WebhookPaymentIntentData("pi_succeeded_test", 1000, "usd", "succeeded", null);
        assertEquals(200, postWebhook(StripeWebhookFixture.paymentSucceeded(data, WEBHOOK_SECRET)).statusCode());

        Thread.sleep(500);

        assertTrue(fetchCaptured("payment-succeeded").contains("\"pi_succeeded_test\""));
    }

    @Test
    void paymentFailed_webhook_isReceivedByConsumer() throws Exception {
        clearCapture();
        final var data = new WebhookPaymentIntentData(
                "pi_failed_test", 1000, "usd", "requires_payment_method", "Your card was declined.");
        assertEquals(200, postWebhook(StripeWebhookFixture.paymentFailed(data, WEBHOOK_SECRET)).statusCode());

        Thread.sleep(500);

        assertTrue(fetchCaptured("payment-failed").contains("\"pi_failed_test\""));
    }

    @Test
    void subscriptionCreated_webhook_isReceivedByConsumer() throws Exception {
        clearCapture();
        final var data = new WebhookSubscriptionData("sub_created_test", "cus_test", "active");
        assertEquals(200, postWebhook(StripeWebhookFixture.subscriptionCreated(data, WEBHOOK_SECRET)).statusCode());

        Thread.sleep(500);

        assertTrue(fetchCaptured("subscription-created").contains("\"sub_created_test\""));
    }

    @Test
    void subscriptionCancelled_webhook_isReceivedByConsumer() throws Exception {
        clearCapture();
        final var data = new WebhookSubscriptionData("sub_cancelled_test", "cus_test", "canceled");
        assertEquals(200, postWebhook(StripeWebhookFixture.subscriptionCancelled(data, WEBHOOK_SECRET)).statusCode());

        Thread.sleep(500);

        assertTrue(fetchCaptured("subscription-cancelled").contains("\"sub_cancelled_test\""));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static HttpResponse<String> postWebhook(StripeWebhookFixture.SignedRequest req) throws Exception {
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(STRIPE_BASE + "/stripe/webhook"))
                .header("Content-Type", "application/json")
                .header("Stripe-Signature", req.signatureHeader())
                .POST(HttpRequest.BodyPublishers.ofString(req.payload()))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String fetchCaptured(String eventType) throws Exception {
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(CONSUMER_BASE + "/captured-events/" + eventType))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private static void clearCapture() throws Exception {
        http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(CONSUMER_BASE + "/captured-events"))
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.discarding()
        );
    }

}
