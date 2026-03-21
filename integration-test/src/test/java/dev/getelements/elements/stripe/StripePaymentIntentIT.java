package dev.getelements.elements.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.service.StripeServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class StripePaymentIntentIT {

    @BeforeAll
    static void setUp() {

        final String apiKey = System.getProperty("stripe.test.apiKey");

        assumeTrue(apiKey != null && !apiKey.isBlank(), "stripe.test.apiKey not provided - skipping live tests");

        Stripe.apiKey = apiKey;
    }

    @Test
    void createPaymentIntent_succeeds() throws StripeException {

        final var request = new CreatePaymentIntentRequest(1000L, "usd", null);
        final var service = new StripeServiceImpl(new LiveStripeGateway());
        final var response = service.createPaymentIntent(request);

        assertNotNull(response.clientSecret());
        assertTrue(response.paymentIntentId().startsWith("pi_"),
                "Expected paymentIntentId to start with 'pi_', got: " + response.paymentIntentId());
    }

    @Test
    void createPaymentIntent_invalidCurrency_throws() {

        final var request = new CreatePaymentIntentRequest(1000L, "XXX", null);
        final var service = new StripeServiceImpl(new LiveStripeGateway());

        assertThrows(jakarta.ws.rs.InternalServerErrorException.class,
                () -> service.createPaymentIntent(request));
    }

}
