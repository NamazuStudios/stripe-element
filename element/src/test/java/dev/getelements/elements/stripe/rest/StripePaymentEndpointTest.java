package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.CreateCheckoutSessionRequest;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionResponse;
import dev.getelements.elements.stripe.model.CreateCustomerResponse;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreatePortalSessionResponse;
import dev.getelements.elements.stripe.model.CreateSubscriptionRequest;
import dev.getelements.elements.stripe.model.InvoiceSummary;
import dev.getelements.elements.stripe.model.PriceSummary;
import dev.getelements.elements.stripe.model.ProductSummary;
import dev.getelements.elements.stripe.model.RecordMeterEventRequest;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.model.UpdateCustomerRequest;
import dev.getelements.elements.stripe.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentEndpointTest {

    @Mock private StripeService stripeService;

    private StripePaymentEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripePaymentEndpoint(stripeService);
    }

    @Test
    void createPaymentIntent_delegatesToServiceAndReturnsResult() {
        final var request = CreatePaymentIntentRequest.of(1000L, "usd", "cus_test");
        final var expected = new CreatePaymentIntentResponse("pi_test", "pi_test_secret");
        when(stripeService.createPaymentIntent(request)).thenReturn(expected);

        assertSame(expected, endpoint.createPaymentIntent(request, null));
        verify(stripeService).createPaymentIntent(request);
    }

    @Test
    void createPaymentIntent_withModeHeader_delegatesToModeAwareOverload() {
        final var request = CreatePaymentIntentRequest.of(1000L, "usd", "cus_test");
        final var expected = new CreatePaymentIntentResponse("pi_test", "pi_test_secret");
        when(stripeService.createPaymentIntent(request, StripeMode.SANDBOX)).thenReturn(expected);

        assertSame(expected, endpoint.createPaymentIntent(request, "sandbox"));
        verify(stripeService).createPaymentIntent(request, StripeMode.SANDBOX);
    }

    @Test
    void invalidModeHeader_throwsBadRequestException() {
        final var request = CreatePaymentIntentRequest.of(1000L, "usd", "cus_test");
        assertThrows(jakarta.ws.rs.BadRequestException.class,
                () -> endpoint.createPaymentIntent(request, "not-a-mode"));
    }

    @Test
    void updateCustomer_callsServiceAndReturns204() {
        final var request = new UpdateCustomerRequest("new@example.com", "New Name");

        final var response = endpoint.updateCustomer("cus_test", request, null);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(stripeService).updateCustomer("cus_test", "new@example.com", "New Name");
    }

    @Test
    void getSubscriptionStatus_delegatesToServiceAndReturnsResult() {
        final var expected = new SubscriptionStatusResponse("sub_test", "active", "2029-12-31T00:00:00Z");
        when(stripeService.getSubscriptionStatus("sub_test")).thenReturn(expected);

        assertSame(expected, endpoint.getSubscriptionStatus("sub_test", null));
    }

    @Test
    void cancelSubscription_delegatesToServiceAndReturnsResult() {
        final var expected = new SubscriptionStatusResponse("sub_test", "canceled", null);
        when(stripeService.cancelSubscription("sub_test")).thenReturn(expected);

        final var result = endpoint.cancelSubscription("sub_test", null);

        assertSame(expected, result);
        verify(stripeService).cancelSubscription("sub_test");
    }

    @Test
    void listSubscriptionsByCustomer_delegatesToServiceAndReturnsResult() {
        final var expected = new SubscriptionListResponse(
                List.of(new SubscriptionStatusResponse("sub_1", "active", "2029-12-31T00:00:00Z")),
                false, "sub_1");
        when(stripeService.listSubscriptionsByCustomer("cus_test", "active", 10, null)).thenReturn(expected);

        assertSame(expected, endpoint.listSubscriptionsByCustomer("cus_test", "active", 10, null, null));
    }

    @Test
    void createPortalSession_delegatesToServiceAndReturnsResult() {
        when(stripeService.createBillingPortalSession("cus_test", "https://example.com/return"))
                .thenReturn("https://billing.stripe.com/session/test");

        final var result = endpoint.createPortalSession("cus_test", "https://example.com/return", null);
        assertEquals("https://billing.stripe.com/session/test", result.url());
    }

    @Test
    void listProducts_delegatesToServiceAndReturnsResult() {
        final var expected = List.of(
                new ProductSummary("prod_001", "Gold Plan", "Our best plan", true,
                        new PriceSummary("price_001", "prod_001", "Gold Monthly", 999L, "usd", "recurring", "month")));
        when(stripeService.listProducts(true, 100)).thenReturn(expected);

        assertSame(expected, endpoint.listProducts(true, 100, null));
    }

    @Test
    void listPrices_delegatesToServiceAndReturnsResult() {
        final var expected = List.of(
                new PriceSummary("price_001", "prod_001", "Gold Monthly", 999L, "usd", "recurring", "month"));
        when(stripeService.listPrices("prod_001", true, 100)).thenReturn(expected);

        assertSame(expected, endpoint.listPrices("prod_001", true, 100, null));
    }

    @Test
    void retrievePrice_delegatesToServiceAndReturnsResult() {
        final var expected = new PriceSummary("price_001", "prod_001", "Gold Monthly", 999L, "usd", "recurring", "month");
        when(stripeService.retrievePrice("price_001")).thenReturn(expected);

        assertSame(expected, endpoint.retrievePrice("price_001", null));
    }

    @Test
    void findCustomerByMetadata_found_returnsCustomerId() {
        when(stripeService.findCustomerByMetadata("orgId", "org_001"))
                .thenReturn(Optional.of("cus_found"));

        assertEquals("cus_found", endpoint.findCustomerByMetadata("orgId", "org_001", null).customerId());
    }

    @Test
    void findCustomerByMetadata_notFound_throwsNotFoundException() {
        when(stripeService.findCustomerByMetadata("orgId", "org_missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> endpoint.findCustomerByMetadata("orgId", "org_missing", null));
    }

    @Test
    void createSubscription_delegatesToServiceAndReturnsResult() {
        final var request = CreateSubscriptionRequest.of("price_abc");
        final var expected = new SubscriptionStatusResponse("sub_new", "active", "2030-01-01T00:00:00Z");
        when(stripeService.createSubscription("cus_test", request)).thenReturn(expected);

        assertSame(expected, endpoint.createSubscription("cus_test", request, null));
        verify(stripeService).createSubscription("cus_test", request);
    }

    @Test
    void listInvoices_delegatesToServiceAndReturnsResult() {
        final var expected = List.of(
                new InvoiceSummary("in_001", "sub_001", 999L, "usd", "paid", "2030-01-01T00:00:00Z"));
        when(stripeService.listInvoices("cus_test", 10, null)).thenReturn(expected);

        assertSame(expected, endpoint.listInvoices("cus_test", 10, null, null));
        verify(stripeService).listInvoices("cus_test", 10, null);
    }

    @Test
    void createCheckoutSession_delegatesToServiceAndReturnsResult() {
        final var request = new CreateCheckoutSessionRequest(
                "cus_test", "price_001",
                "https://example.com/success", "https://example.com/cancel", null, null, null);
        final var expected = new CreateCheckoutSessionResponse("cs_test", "https://checkout.stripe.com/pay/cs_test");
        when(stripeService.createCheckoutSession(request)).thenReturn(expected);

        assertSame(expected, endpoint.createCheckoutSession(request, null));
    }

    @Test
    void recordMeterEvent_delegatesToServiceAndReturns204() {
        final var request = new RecordMeterEventRequest("cus_test", "api_requests", 10, "idem-key-1");

        final var response = endpoint.recordMeterEvent(request, null);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(stripeService).recordMeterEvent("cus_test", "api_requests", BigDecimal.valueOf(10), "idem-key-1");
    }

}
