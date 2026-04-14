package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.sdk.Event;
import dev.getelements.elements.stripe.event.StripeInvoicePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripeInvoicePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripePaymentFailedEvent;
import dev.getelements.elements.stripe.event.StripePaymentSucceededEvent;
import dev.getelements.elements.stripe.event.StripeRawEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCancelledEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionCreatedEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionTrialWillEndEvent;
import dev.getelements.elements.stripe.event.StripeSubscriptionUpdatedEvent;
import dev.getelements.elements.stripe.service.StripeService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookEndpointTest {

    private static final String TEST_SECRET = "whsec_unit_test_secret_only";

    @Mock
    private Element element;

    @Mock
    private StripeService stripeService;

    private StripeWebhookEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripeWebhookEndpoint(element, TEST_SECRET, stripeService);
    }

    // --- signature verification ---

    @Test
    void invalidSignature_returns400() {

        final var response = endpoint.receiveWebhook("{}", "t=0,v1=badsig");

        assertEquals(400, response.getStatus());
        verifyNoMoreInteractions(element);
    }

    // --- raw event: always published ---

    @Test
    void anyKnownEvent_alsoPublishesRawEvent() throws Exception {

        final String payload = """
                {"id":"evt_raw_1","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"payment_intent.succeeded",
                 "data":{"object":{"id":"pi_raw","object":"payment_intent",
                 "amount":100,"currency":"usd","status":"succeeded"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var raw = captureAllPublished().stream()
                .filter(e -> e instanceof StripeRawEvent)
                .map(e -> (StripeRawEvent) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("StripeRawEvent not published"));

        assertEquals("payment_intent.succeeded", raw.type());
        assertEquals("evt_raw_1", raw.eventId());
        assertEquals(payload, raw.rawJson());
    }

    @Test
    void unknownEventType_publishesOnlyRawEvent() throws Exception {

        final String payload = """
                {"id":"evt_unknown_1","object":"event","type":"charge.succeeded",
                 "data":{"object":{"id":"ch_001","object":"charge"}}}""";

        final var response = endpoint.receiveWebhook(payload, sig(payload));

        assertEquals(200, response.getStatus());

        final var published = captureAllPublished();
        assertEquals(1, published.size(), "Expected only the raw event to be published");

        final var raw = (StripeRawEvent) published.get(0);
        assertEquals("charge.succeeded", raw.type());
        assertEquals("evt_unknown_1", raw.eventId());
    }

    // --- payment_intent.succeeded ---

    @Test
    void paymentIntentSucceeded_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_1","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"payment_intent.succeeded",
                 "data":{"object":{"id":"pi_001","object":"payment_intent",
                 "amount":2500,"currency":"eur","status":"succeeded",
                 "metadata":{"userId":"user_abc"}}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var published = captureAllPublished();
        final var typed = assertSingleTyped(published, StripePaymentSucceededEvent.class);

        assertEquals("pi_001", typed.paymentIntentId());
        assertEquals(2500L, typed.amount());
        assertEquals("eur", typed.currency());
        verify(stripeService).recordPaymentReceipt("pi_001", 2500L, "eur", "user_abc");
    }

    @Test
    void paymentIntentSucceeded_noUserId_skipsReceipt() throws Exception {

        final String payload = """
                {"id":"evt_1b","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"payment_intent.succeeded",
                 "data":{"object":{"id":"pi_002","object":"payment_intent",
                 "amount":1000,"currency":"usd","status":"succeeded"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        verify(stripeService).recordPaymentReceipt("pi_002", 1000L, "usd", null);
    }

    // --- payment_intent.payment_failed ---

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

        final var typed = assertSingleTyped(captureAllPublished(), StripePaymentFailedEvent.class);

        assertEquals("pi_002", typed.paymentIntentId());
        assertEquals("Your card was declined.", typed.failureMessage());
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

        final var typed = assertSingleTyped(captureAllPublished(), StripePaymentFailedEvent.class);
        assertEquals("Unknown failure", typed.failureMessage());
    }

    // --- invoice.payment_succeeded ---

    @Test
    void invoicePaymentSucceeded_publishesTypedEventAndSavesReceipt() throws Exception {

        final String payload = """
                {"id":"evt_inv_1","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"invoice.payment_succeeded",
                 "data":{"object":{"id":"in_001","object":"invoice",
                 "payment_intent":"pi_inv_001","amount_paid":999,"currency":"usd",
                 "metadata":{"userId":"user_xyz"}}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeInvoicePaymentSucceededEvent.class);

        assertEquals("in_001", typed.invoiceId());
        assertEquals("pi_inv_001", typed.paymentIntentId());
        assertEquals(999L, typed.amountPaid());
        assertEquals("usd", typed.currency());
        verify(stripeService).recordPaymentReceipt("pi_inv_001", 999L, "usd", "user_xyz");
    }

    // --- invoice.payment_failed ---

    @Test
    void invoicePaymentFailed_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_inv_fail_1","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"invoice.payment_failed",
                 "data":{"object":{"id":"in_002","object":"invoice",
                 "subscription":"sub_fail_001","customer":"cus_fail_001",
                 "last_finalization_error":{"message":"Your card has insufficient funds."}}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeInvoicePaymentFailedEvent.class);

        assertEquals("in_002", typed.invoiceId());
        assertEquals("sub_fail_001", typed.subscriptionId());
        assertEquals("cus_fail_001", typed.customerId());
        assertEquals("Your card has insufficient funds.", typed.failureMessage());
    }

    @Test
    void invoicePaymentFailed_noErrorObject_usesDefaultMessage() throws Exception {

        final String payload = """
                {"id":"evt_inv_fail_2","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"invoice.payment_failed",
                 "data":{"object":{"id":"in_003","object":"invoice",
                 "subscription":"sub_fail_002","customer":"cus_fail_002"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeInvoicePaymentFailedEvent.class);
        assertEquals("Payment failed", typed.failureMessage());
    }

    // --- customer.subscription.created ---

    @Test
    void subscriptionCreated_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_4","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"customer.subscription.created",
                 "data":{"object":{"id":"sub_001","object":"subscription",
                 "customer":"cus_001","status":"active"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeSubscriptionCreatedEvent.class);

        assertEquals("sub_001", typed.subscriptionId());
        assertEquals("cus_001", typed.customerId());
        assertEquals("active", typed.status());
    }

    // --- customer.subscription.updated ---

    @Test
    void subscriptionUpdated_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_upd_1","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"customer.subscription.updated",
                 "data":{"object":{"id":"sub_upd_001","object":"subscription",
                 "customer":"cus_upd_001","status":"active"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeSubscriptionUpdatedEvent.class);

        assertEquals("sub_upd_001", typed.subscriptionId());
        assertEquals("cus_upd_001", typed.customerId());
        assertEquals("active", typed.status());
    }

    // --- customer.subscription.deleted ---

    @Test
    void subscriptionDeleted_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_5","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"customer.subscription.deleted",
                 "data":{"object":{"id":"sub_002","object":"subscription",
                 "customer":"cus_002","status":"canceled"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeSubscriptionCancelledEvent.class);

        assertEquals("sub_002", typed.subscriptionId());
        assertEquals("cus_002", typed.customerId());
    }

    // --- customer.subscription.trial_will_end ---

    @Test
    void subscriptionTrialWillEnd_publishesCorrectEvent() throws Exception {

        final String payload = """
                {"id":"evt_trial_1","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"customer.subscription.trial_will_end",
                 "data":{"object":{"id":"sub_trial_001","object":"subscription",
                 "customer":"cus_trial_001","status":"trialing",
                 "trial_end":1893456000}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeSubscriptionTrialWillEndEvent.class);

        assertEquals("sub_trial_001", typed.subscriptionId());
        assertEquals("cus_trial_001", typed.customerId());
        assertNotNull(typed.trialEnd());
        assertTrue(typed.trialEnd().startsWith("2030-"));
    }

    @Test
    void subscriptionTrialWillEnd_nullTrialEnd_publishesNullTrialEnd() throws Exception {

        final String payload = """
                {"id":"evt_trial_2","object":"event","api_version":"2024-04-10",
                 "created":1234567890,"livemode":false,
                 "type":"customer.subscription.trial_will_end",
                 "data":{"object":{"id":"sub_trial_002","object":"subscription",
                 "customer":"cus_trial_002","status":"trialing"}}}""";

        endpoint.receiveWebhook(payload, sig(payload));

        final var typed = assertSingleTyped(captureAllPublished(), StripeSubscriptionTrialWillEndEvent.class);
        assertNull(typed.trialEnd());
    }

    // --- helpers ---

    private List<Event> captureAllPublished() {
        final var captor = ArgumentCaptor.forClass(Event.class);
        verify(element, atLeast(1)).publish(captor.capture());
        return captor.getAllValues();
    }

    private <T> T assertSingleTyped(List<Event> published, Class<T> type) {
        return published.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .reduce((a, b) -> { throw new AssertionError("Multiple " + type.getSimpleName() + " published"); })
                .orElseThrow(() -> new AssertionError("No " + type.getSimpleName() + " published"));
    }

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
