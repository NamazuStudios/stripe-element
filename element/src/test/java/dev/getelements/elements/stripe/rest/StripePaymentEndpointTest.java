package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.CreatePortalSessionResponse;
import dev.getelements.elements.stripe.model.SubscriptionListResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import dev.getelements.elements.stripe.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentEndpointTest {

    @Mock
    private StripeService stripeService;

    private StripePaymentEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripePaymentEndpoint(stripeService);
    }

    @Test
    void createPaymentIntent_delegatesToServiceAndReturnsResult() {

        final var request = new CreatePaymentIntentRequest(1000L, "usd", "cus_test");
        final var expected = new CreatePaymentIntentResponse("pi_test", "pi_test_secret");

        when(stripeService.createPaymentIntent(request)).thenReturn(expected);

        assertSame(expected, endpoint.createPaymentIntent(request));
        verify(stripeService).createPaymentIntent(request);
    }

    @Test
    void getSubscriptionStatus_delegatesToServiceAndReturnsResult() {

        final var expected = new SubscriptionStatusResponse("sub_test", "active", "2029-12-31T00:00:00Z");

        when(stripeService.getSubscriptionStatus("sub_test")).thenReturn(expected);

        assertSame(expected, endpoint.getSubscriptionStatus("sub_test"));
        verify(stripeService).getSubscriptionStatus("sub_test");
    }

    @Test
    void listSubscriptionsByCustomer_delegatesToServiceAndReturnsResult() {

        final var expected = new SubscriptionListResponse(
                List.of(
                        new SubscriptionStatusResponse("sub_1", "active", "2029-12-31T00:00:00Z"),
                        new SubscriptionStatusResponse("sub_2", "past_due", "2029-11-30T00:00:00Z")
                ),
                false,
                "sub_2"
        );

        when(stripeService.listSubscriptionsByCustomer("cus_test", "active", 10, null)).thenReturn(expected);

        assertSame(expected, endpoint.listSubscriptionsByCustomer("cus_test", "active", 10, null));
        verify(stripeService).listSubscriptionsByCustomer("cus_test", "active", 10, null);
    }

    @Test
    void createPortalSession_delegatesToServiceAndReturnsResult() {

        when(stripeService.createBillingPortalSession("cus_test", "https://example.com/return"))
                .thenReturn("https://billing.stripe.com/session/test");

        final var result = endpoint.createPortalSession("cus_test", "https://example.com/return");
        assertEquals("https://billing.stripe.com/session/test", result.url());
        verify(stripeService).createBillingPortalSession("cus_test", "https://example.com/return");
    }

    @Test
    void createPortalSession_nullReturnUrl_passesNullToService() {

        when(stripeService.createBillingPortalSession("cus_test", null))
                .thenReturn("https://billing.stripe.com/session/test2");

        final var result = endpoint.createPortalSession("cus_test", null);
        assertEquals("https://billing.stripe.com/session/test2", result.url());
    }

    @Test
    void listSubscriptionsByCustomer_withPagination_passesParamsThrough() {

        final var expected = new SubscriptionListResponse(List.of(), false, null);

        when(stripeService.listSubscriptionsByCustomer("cus_test", null, 25, "sub_cursor")).thenReturn(expected);

        assertSame(expected, endpoint.listSubscriptionsByCustomer("cus_test", null, 25, "sub_cursor"));
        verify(stripeService).listSubscriptionsByCustomer("cus_test", null, 25, "sub_cursor");
    }

}
