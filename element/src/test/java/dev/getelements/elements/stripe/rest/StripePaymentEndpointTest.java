package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.CreatePaymentIntentResponse;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import dev.getelements.elements.stripe.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

}
