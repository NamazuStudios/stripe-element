package dev.getelements.elements.stripe.consumer;

import com.stripe.Stripe;
import dev.getelements.elements.stripe.model.WebhookPaymentIntentData;
import dev.getelements.elements.stripe.model.WebhookSubscriptionData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Builds signed Stripe webhook HTTP requests from typed model objects, eliminating raw JSON
 * strings from integration tests.
 *
 * <p>Usage:
 * <pre>{@code
 * var data = new WebhookPaymentIntentData("pi_test", 1000, "usd", "succeeded", null);
 * var req  = StripeWebhookFixture.paymentSucceeded(data, WEBHOOK_SECRET);
 * postWebhook(req.payload(), req.signatureHeader());
 * }</pre>
 */
public final class StripeWebhookFixture {

    private StripeWebhookFixture() {}

    /** A signed webhook payload ready to send as an HTTP request body + header. */
    public record SignedRequest(String payload, String signatureHeader) {}

    public static SignedRequest paymentSucceeded(WebhookPaymentIntentData data, String secret) throws Exception {
        final var payload = paymentIntentPayload(data, "payment_intent.succeeded");
        return new SignedRequest(payload, sign(payload, secret));
    }

    public static SignedRequest paymentFailed(WebhookPaymentIntentData data, String secret) throws Exception {
        final var payload = paymentIntentPayload(data, "payment_intent.payment_failed");
        return new SignedRequest(payload, sign(payload, secret));
    }

    public static SignedRequest subscriptionCreated(WebhookSubscriptionData data, String secret) throws Exception {
        final var payload = subscriptionPayload(data, "customer.subscription.created");
        return new SignedRequest(payload, sign(payload, secret));
    }

    public static SignedRequest subscriptionCancelled(WebhookSubscriptionData data, String secret) throws Exception {
        final var payload = subscriptionPayload(data, "customer.subscription.deleted");
        return new SignedRequest(payload, sign(payload, secret));
    }

    // -------------------------------------------------------------------------

    private static String paymentIntentPayload(WebhookPaymentIntentData d, String type) {
        final var failureBlock = d.failureMessage() != null
                ? "\"last_payment_error\":{\"message\":\"%s\"}".formatted(d.failureMessage())
                : "\"last_payment_error\":null";
        return """
                {"id":"evt_fixture","object":"event","api_version":"%s","type":"%s",
                 "data":{"object":{"id":"%s","object":"payment_intent",
                 "amount":%d,"currency":"%s","status":"%s",%s}}}"""
                .formatted(Stripe.API_VERSION, type,
                        d.id(), d.amount(), d.currency(), d.status(), failureBlock);
    }

    private static String subscriptionPayload(WebhookSubscriptionData d, String type) {
        return """
                {"id":"evt_fixture","object":"event","api_version":"%s","type":"%s",
                 "data":{"object":{"id":"%s","object":"subscription",
                 "customer":"%s","status":"%s"}}}"""
                .formatted(Stripe.API_VERSION, type, d.id(), d.customerId(), d.status());
    }

    private static String sign(String payload, String secret) throws Exception {
        final long ts = System.currentTimeMillis() / 1000L;
        final var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        final var hash = mac.doFinal((ts + "." + payload).getBytes(StandardCharsets.UTF_8));
        final var hex = new StringBuilder();
        for (var b : hash) hex.append(String.format("%02x", b));
        return "t=" + ts + ",v1=" + hex;
    }

}
