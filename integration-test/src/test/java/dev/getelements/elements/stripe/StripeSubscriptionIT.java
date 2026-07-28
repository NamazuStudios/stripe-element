package dev.getelements.elements.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceUpdateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.ProductUpdateParams;
import com.stripe.param.SubscriptionCreateParams;
import dev.getelements.elements.stripe.service.StripeMeterPriceCache;
import dev.getelements.elements.stripe.service.StripePriceCache;
import dev.getelements.elements.stripe.service.StripeServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

public class StripeSubscriptionIT {

    private static String customerId;
    private static String subscriptionId;
    private static String createdProductId;
    private static String createdPriceId;

    @BeforeAll
    static void setUp() throws StripeException {

        final String apiKey = System.getProperty("stripe.test.apiKey");

        assumeTrue(apiKey != null && !apiKey.isBlank(), "stripe.test.apiKey not provided - skipping");

        Stripe.apiKey = apiKey;

        // Resolve price: use supplied ID or create a one-off test price
        var priceId = System.getProperty("stripe.test.priceId");

        if (priceId == null || priceId.isBlank()) {

            final var product = Product.create(ProductCreateParams.builder()
                    .setName("stripe-element-it-product")
                    .build());
            createdProductId = product.getId();

            final var price = Price.create(PriceCreateParams.builder()
                    .setProduct(createdProductId)
                    .setCurrency("usd")
                    .setUnitAmount(999L)
                    .setRecurring(PriceCreateParams.Recurring.builder()
                            .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                            .build())
                    .build());
            createdPriceId = price.getId();

            priceId = createdPriceId;
        }

        final var customer = Customer.create(CustomerCreateParams.builder()
                .setDescription("stripe-element-it-customer")
                .build());

        customerId = customer.getId();

        final var subscription = Subscription.create(SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .build());

        subscriptionId = subscription.getId();
    }

    @AfterAll
    static void tearDown() throws StripeException {

        if (subscriptionId != null) {
            Subscription.retrieve(subscriptionId).cancel();
        }

        if (customerId != null) {
            Customer.retrieve(customerId).delete();
        }

        // Archive the price and product created by this run (Stripe forbids deletion of
        // products that have associated prices, so we deactivate instead).
        if (createdPriceId != null) {
            Price.retrieve(createdPriceId).update(
                    PriceUpdateParams.builder().setActive(false).build());
        }

        if (createdProductId != null) {
            Product.retrieve(createdProductId).update(
                    ProductUpdateParams.builder().setActive(false).build());
        }
    }

    @Test
    void getSubscriptionStatus_returnsStatus() {

        final var service = new StripeServiceImpl(new LiveStripeGateway(), mock(StripePriceCache.class), mock(StripeMeterPriceCache.class), null, null);
        final var response = service.getSubscriptionStatus(subscriptionId);

        assertNotNull(response.subscriptionId());
        assertNotNull(response.status());
    }

    @Test
    void getSubscriptionStatus_matchesDirectRetrieve() throws StripeException {

        final var direct = Subscription.retrieve(subscriptionId);
        final var service = new StripeServiceImpl(new LiveStripeGateway(), mock(StripePriceCache.class), mock(StripeMeterPriceCache.class), null, null);
        final var response = service.getSubscriptionStatus(subscriptionId);

        assertEquals(direct.getStatus(), response.status());
        assertEquals(direct.getId(), response.subscriptionId());
    }

}
