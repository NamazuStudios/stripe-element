package dev.getelements.elements.stripe.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.ws.rs.InternalServerErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceImplTest {

    @Mock
    private StripeGateway gateway;

    private StripeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StripeServiceImpl(gateway);
    }

    @Test
    void createPaymentIntent_mapsResponseCorrectly() throws StripeException {

        final var pi = mock(PaymentIntent.class);

        when(pi.getId()).thenReturn("pi_test_001");
        when(pi.getClientSecret()).thenReturn("pi_test_001_secret");
        when(gateway.createPaymentIntent(any())).thenReturn(pi);

        final var response = service.createPaymentIntent(new CreatePaymentIntentRequest(1500L, "usd", "cus_test"));

        assertEquals("pi_test_001", response.paymentIntentId());
        assertEquals("pi_test_001_secret", response.clientSecret());
    }

    @Test
    void createPaymentIntent_passesParamsToGateway() throws StripeException {

        final var pi = mock(PaymentIntent.class);

        when(gateway.createPaymentIntent(any())).thenReturn(pi);

        service.createPaymentIntent(new CreatePaymentIntentRequest(2000L, "eur", "cus_eu"));

        verify(gateway).createPaymentIntent(argThat(p ->
                p.getAmount() == 2000L
                && "eur".equals(p.getCurrency())
                && "cus_eu".equals(p.getCustomer())
        ));
    }

    @Test
    void createPaymentIntent_stripeException_wrapsAsInternalServerError() throws StripeException {

        when(gateway.createPaymentIntent(any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.createPaymentIntent(new CreatePaymentIntentRequest(1000L, "usd", null)));
    }

    @Test
    void getSubscriptionStatus_mapsResponseCorrectly() throws StripeException {

        final var sub = mock(Subscription.class);

        when(sub.getId()).thenReturn("sub_test_001");
        when(sub.getStatus()).thenReturn("active");
        when(sub.getCurrentPeriodEnd()).thenReturn(1893456000L); // 2030-01-01
        when(gateway.retrieveSubscription("sub_test_001")).thenReturn(sub);

        final var response = service.getSubscriptionStatus("sub_test_001");

        assertEquals("sub_test_001", response.subscriptionId());
        assertEquals("active", response.status());
        assertNotNull(response.currentPeriodEnd());
        assertTrue(response.currentPeriodEnd().startsWith("2030-"));
    }

    @Test
    void getSubscriptionStatus_nullPeriodEnd_returnedAsNull() throws StripeException {

        final var sub = mock(Subscription.class);

        when(sub.getId()).thenReturn("sub_test_002");
        when(sub.getStatus()).thenReturn("canceled");
        when(sub.getCurrentPeriodEnd()).thenReturn(null);
        when(gateway.retrieveSubscription("sub_test_002")).thenReturn(sub);

        final SubscriptionStatusResponse response = service.getSubscriptionStatus("sub_test_002");

        assertNull(response.currentPeriodEnd());
    }

    @Test
    void getSubscriptionStatus_stripeException_wrapsAsInternalServerError() throws StripeException {

        when(gateway.retrieveSubscription(any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.getSubscriptionStatus("sub_missing"));
    }

}
