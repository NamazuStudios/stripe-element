package dev.getelements.elements.stripe;

import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.event.StripeSubscriptionCancelledEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCreatedEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionTrialWillEndEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionUpdatedEvent;
import dev.getelements.elements.stripe.rest.StripeWebhookEndpoint;
import dev.getelements.elements.stripe.service.StripeService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies that the webhook handler correctly extracts {@code orgId} from Stripe subscription
 * metadata and forwards it in the typed event. Uses a hardcoded test secret — no live API key required.
 */
public class StripeWebhookEventDispatchIT {

    private static final String TEST_SECRET = "whsec_test_secret_for_dispatch_testing";

    private static String buildSignatureHeader(String payload, long timestamp) throws Exception {
        final var signedPayload = timestamp + "." + payload;
        final var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        final var hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        final var hex = new StringBuilder();
        for (var b : hash) hex.append(String.format("%02x", b));
        return "t=" + timestamp + ",v1=" + hex;
    }

    private static Response post(Element element, String payload) throws Exception {
        final var timestamp = System.currentTimeMillis() / 1000L;
        final var sig = buildSignatureHeader(payload, timestamp);
        return new StripeWebhookEndpoint(element, TEST_SECRET, mock(StripeService.class))
                .receiveWebhook(payload, sig);
    }

    private static Event capturePublished(Element element) {
        final var captor = ArgumentCaptor.forClass(Event.class);
        // publish() is called twice: once for the raw event, once for the typed subscription event.
        verify(element, atLeastOnce()).publish(captor.capture());
        final var values = captor.getAllValues();
        return values.get(values.size() - 1);
    }

    // ---- subscription.created -----------------------------------------------

    @Test
    void subscriptionCreated_withOrgId_propagatesOrgId() throws Exception {
        final var element = mock(Element.class);
        final var payload = subscriptionPayload("customer.subscription.created", "org_123");

        final var response = post(element, payload);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final var event = (StripeSubscriptionCreatedEvent) capturePublished(element);
        assertEquals("sub_it_001", event.subscriptionId());
        assertEquals("cus_it_001", event.customerId());
        assertEquals("active", event.status());
        assertEquals("org_123", event.orgId());
    }

    @Test
    void subscriptionCreated_withoutOrgId_orgIdIsNull() throws Exception {
        final var element = mock(Element.class);
        final var payload = subscriptionPayload("customer.subscription.created", null);

        final var response = post(element, payload);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final var event = (StripeSubscriptionCreatedEvent) capturePublished(element);
        assertNull(event.orgId());
    }

    // ---- subscription.updated -----------------------------------------------

    @Test
    void subscriptionUpdated_withOrgId_propagatesOrgId() throws Exception {
        final var element = mock(Element.class);
        final var payload = subscriptionPayload("customer.subscription.updated", "org_456");

        final var response = post(element, payload);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final var event = (StripeSubscriptionUpdatedEvent) capturePublished(element);
        assertEquals("org_456", event.orgId());
    }

    // ---- subscription.deleted (cancelled) -----------------------------------

    @Test
    void subscriptionCancelled_withOrgId_propagatesOrgId() throws Exception {
        final var element = mock(Element.class);
        final var payload = subscriptionPayload("customer.subscription.deleted", "org_789");

        final var response = post(element, payload);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final var event = (StripeSubscriptionCancelledEvent) capturePublished(element);
        assertEquals("org_789", event.orgId());
    }

    // ---- subscription.trial_will_end ----------------------------------------

    @Test
    void subscriptionTrialWillEnd_withOrgId_propagatesOrgId() throws Exception {
        final var element = mock(Element.class);
        final var payload = subscriptionTrialPayload("org_trial");

        final var response = post(element, payload);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final var event = (StripeSubscriptionTrialWillEndEvent) capturePublished(element);
        assertEquals("org_trial", event.orgId());
    }

    // ---- helpers ------------------------------------------------------------

    private static String subscriptionPayload(String type, String orgId) {
        final var metadataJson = orgId != null
                ? String.format("{\"orgId\":\"%s\"}", orgId)
                : "{}";
        return String.format("""
                {
                  "id": "evt_it_sub_001",
                  "object": "event",
                  "api_version": "2024-04-10",
                  "type": "%s",
                  "data": {
                    "object": {
                      "id": "sub_it_001",
                      "object": "subscription",
                      "customer": "cus_it_001",
                      "status": "active",
                      "metadata": %s
                    }
                  }
                }
                """, type, metadataJson);
    }

    private static String subscriptionTrialPayload(String orgId) {
        final var metadataJson = orgId != null
                ? String.format("{\"orgId\":\"%s\"}", orgId)
                : "{}";
        return String.format("""
                {
                  "id": "evt_it_trial_001",
                  "object": "event",
                  "api_version": "2024-04-10",
                  "type": "customer.subscription.trial_will_end",
                  "data": {
                    "object": {
                      "id": "sub_it_001",
                      "object": "subscription",
                      "customer": "cus_it_001",
                      "status": "trialing",
                      "trial_end": 9999999999,
                      "metadata": %s
                    }
                  }
                }
                """, metadataJson);
    }

}
