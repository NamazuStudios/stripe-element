package dev.getelements.elements.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import dev.getelements.elements.sdk.dao.Transaction;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.service.user.UserService;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.service.StripeServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

public class StripePaymentIntentIT {

    @BeforeAll
    static void setUp() {

        final String apiKey = System.getProperty("stripe.test.apiKey");

        assumeTrue(apiKey != null && !apiKey.isBlank(), "stripe.test.apiKey not provided - skipping live tests");

        Stripe.apiKey = apiKey;
    }

    private static StripeServiceImpl service() {
        final UserService userService = mock(UserService.class);
        final var user = mock(User.class);
        when(user.getId()).thenReturn("it_user");
        when(userService.getCurrentUser()).thenReturn(user);
        final Transaction transaction = mock(Transaction.class);
        doAnswer(inv -> null).when(transaction).performAndCloseV(any());
        return new StripeServiceImpl(new LiveStripeGateway(), userService, () -> transaction);
    }

    @Test
    void createPaymentIntent_succeeds() throws StripeException {

        final var request = new CreatePaymentIntentRequest(1000L, "usd", null);
        final var response = service().createPaymentIntent(request);

        assertNotNull(response.clientSecret());
        assertTrue(response.paymentIntentId().startsWith("pi_"),
                "Expected paymentIntentId to start with 'pi_', got: " + response.paymentIntentId());
    }

    @Test
    void createPaymentIntent_invalidCurrency_throws() {

        final var request = new CreatePaymentIntentRequest(1000L, "XXX", null);

        assertThrows(jakarta.ws.rs.InternalServerErrorException.class,
                () -> service().createPaymentIntent(request));
    }

}
