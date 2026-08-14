package dev.getelements.elements.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import dev.getelements.elements.stripe.service.StripeService;
import dev.getelements.elements.stripe.service.StripeServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.getelements.elements.stripe.service.StripeMeterPriceCache;
import dev.getelements.elements.stripe.service.StripePriceCache;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

/**
 * Live integration tests for customer management, SetupIntent creation, and payment method listing.
 * Skipped when {@code stripe.test.apiKey} is not provided.
 */
public class StripeCustomerIT {

    private static String createdCustomerId;

    @BeforeAll
    static void setUp() {
        final String apiKey = System.getProperty("stripe.test.apiKey");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "stripe.test.apiKey not provided - skipping live tests");
        Stripe.apiKey = apiKey;
    }

    @AfterAll
    static void tearDown() throws StripeException {
        if (createdCustomerId != null) {
            Customer.retrieve(createdCustomerId).delete();
        }
    }

    private static StripeServiceImpl service() {
        return new StripeServiceImpl(new LiveStripeGateway(), mock(StripePriceCache.class), mock(StripeMeterPriceCache.class), null, null);
    }

    // ---- createCustomer -----------------------------------------------------

    @Test
    void createCustomer_returnsCustomerId() {
        final var response = service().createCustomer("it-test@example.com", "IT Test Org", "org_it_001");

        assertNotNull(response.customerId());
        assertTrue(response.customerId().startsWith("cus_"),
                "Expected customerId to start with 'cus_', got: " + response.customerId());

        createdCustomerId = response.customerId();
    }

    @Test
    void createCustomer_orgIdStoredInMetadata() throws StripeException {
        final var response = service().createCustomer("it-meta@example.com", "Meta Test Org", "org_meta_001");

        try {
            final var customer = Customer.retrieve(response.customerId());
            assertEquals("org_meta_001", customer.getMetadata().get(StripeService.METADATA_ORG_ID));
        } finally {
            Customer.retrieve(response.customerId()).delete();
        }
    }

    // ---- createSetupIntent --------------------------------------------------

    @Test
    void createSetupIntent_returnsClientSecret() throws StripeException {
        final var customer = Customer.create(
                com.stripe.param.CustomerCreateParams.builder()
                        .setDescription("stripe-element-it-setupintent-customer")
                        .build());

        try {
            final var response = service().createSetupIntent(customer.getId());

            assertNotNull(response.setupIntentId());
            assertNotNull(response.clientSecret());
            assertTrue(response.setupIntentId().startsWith("seti_"),
                    "Expected setupIntentId to start with 'seti_', got: " + response.setupIntentId());
        } finally {
            customer.delete();
        }
    }

    // ---- listPaymentMethods -------------------------------------------------

    @Test
    void listPaymentMethods_newCustomer_returnsEmptyList() throws StripeException {
        final var customer = Customer.create(
                com.stripe.param.CustomerCreateParams.builder()
                        .setDescription("stripe-element-it-pm-customer")
                        .build());

        try {
            final var methods = service().listPaymentMethods(customer.getId());
            assertNotNull(methods);
            assertTrue(methods.isEmpty(), "New customer should have no payment methods");
        } finally {
            customer.delete();
        }
    }

}
