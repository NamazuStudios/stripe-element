package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.stripe.event.StripePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCancelledEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCreatedEvent;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookEndpointTest {

    private static final String TEST_SECRET = "whsec_unit_test_secret_only";

    @Mock
    private Element element;

    private StripeWebhookEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripeWebhookEndpoint(element, TEST_SECRET);
    }

    // --- signature verification ---

    @Test
    void invalidSignature_returns400() {

        final var response = endpoint.receiveWebhook("{}", "t=0,v1=badsig");

        assertEquals(400, response.getStatus());
        verifyNoMoreInteractions(element);
    }

    // --- event dispatch: happy paths ---

    @Test
    void paymentIntentSucceeded_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_1","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"payment_intent.succeeded",
                 "data":{"object":{"id":"pi_001","object":"payment_intent",
                 "amount":2500,"currency":"eur","status":"succeeded"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var captor = ArgumentCaptor.forClass(StripePaymentSucceededEvent.class);

        verify(element).publish(captor.capture());

        assertEquals("pi_001", captor.getValue().paymentIntentId());
        assertEquals(2500L, captor.getValue().amount());
        assertEquals("eur", captor.getValue().currency());
    }

    @Test
    void paymentIntentFailed_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_2","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"payment_intent.payment_failed",
                 "data":{"object":{"id":"pi_002","object":"payment_intent",
                 "amount":1000,"currency":"usd","status":"requires_payment_method",
                 "last_payment_error":{"message":"Your card was declined."}}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var captor = ArgumentCaptor.forClass(StripePaymentFailedEvent.class);

        verify(element).publish(captor.capture());

        assertEquals("pi_002", captor.getValue().paymentIntentId());
        assertEquals("Your card was declined.", captor.getValue().failureMessage());
    }

    @Test
    void paymentIntentFailed_noErrorObject_usesDefaultMessage() throws Exception {

        final String payload = """
                {"id":"evt_3","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"payment_intent.payment_failed",
                 "data":{"object":{"id":"pi_003","object":"payment_intent",
                 "amount":1000,"currency":"usd","status":"requires_payment_method"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var captor = ArgumentCaptor.forClass(StripePaymentFailedEvent.class);

        verify(element).publish(captor.capture());
        assertEquals("Unknown failure", captor.getValue().failureMessage());
    }

    @Test
    void subscriptionCreated_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_4","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"customer.subscription.created",
                 "data":{"object":{"id":"sub_001","object":"subscription",
                 "customer":"cus_001","status":"active"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var captor = ArgumentCaptor.forClass(StripeSubscriptionCreatedEvent.class);

        verify(element).publish(captor.capture());

        assertEquals("sub_001", captor.getValue().subscriptionId());
        assertEquals("cus_001", captor.getValue().customerId());
        assertEquals("active", captor.getValue().status());
    }

    @Test
    void subscriptionDeleted_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_5","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"customer.subscription.deleted",
                 "data":{"object":{"id":"sub_002","object":"subscription",
                 "customer":"cus_002","status":"canceled"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var captor = ArgumentCaptor.forClass(StripeSubscriptionCancelledEvent.class);

        verify(element).publish(captor.capture());

        assertEquals("sub_002", captor.getValue().subscriptionId());
        assertEquals("cus_002", captor.getValue().customerId());
    }

    @Test
    void unknownEventType_returnsOkWithoutPublishing() throws Exception {

        final String payload = """
                {"id":"evt_6","object":"event","type":"charge.succeeded",
                 "data":{"object":{"id":"ch_001","object":"charge"}}}""";

        final var response = endpoint.receiveWebhook(payload, sig(payload));

        assertEquals(200, response.getStatus());

        verify(element, never()).publish(any());
    }

    // --- helpers ---

    private String sig(String payload) throws Exception {

        final var ts = System.currentTimeMillis() / 1000L;
        final var signed = ts + "." + payload;
        final var algorithm = "HmacSHA256";
        final var mac = Mac.getInstance(algorithm);

        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), algorithm));

        final var hash = mac.doFinal(signed.getBytes(StandardCharsets.UTF_8));
        final var hex = new StringBuilder();

        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }

        return "t=" + ts + ",v1=" + hex;
    }

}
