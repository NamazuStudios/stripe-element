package dev.getelements.elements.stripe.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.billingportal.Session;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.billingportal.SessionCreateParams;
import dev.getelements.elements.stripe.model.CreateCheckoutSessionRequest;
import dev.getelements.elements.stripe.model.CreateSubscriptionRequest;
import dev.getelements.elements.stripe.model.InvoiceSummary;
import dev.getelements.elements.stripe.model.PriceSummary;
import dev.getelements.elements.sdk.dao.ReceiptDao;
import dev.getelements.elements.sdk.dao.Transaction;
import dev.getelements.elements.sdk.model.receipt.Receipt;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.service.user.UserService;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.inject.Provider;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceImplTest {

    @Mock private StripeGateway gateway;
    @Mock private StripePriceCache priceCache;
    @Mock private SubscriptionCollection subscriptionCollection;
    @Mock private PriceCollection priceCollection;
    @Mock private com.stripe.model.ProductCollection productCollection;
    @Mock private InvoiceCollection invoiceCollection;
    @Mock private UserService userService;
    @Mock private Provider<Transaction> transactionProvider;
    @Mock private Transaction transaction;
    @Mock private ReceiptDao receiptDao;

    private StripeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StripeServiceImpl(gateway, priceCache, () -> userService, transactionProvider);
        final var user = mock(User.class);
        lenient().when(user.getId()).thenReturn("user_test_001");
        lenient().when(userService.getCurrentUser()).thenReturn(user);
        lenient().when(transactionProvider.get()).thenReturn(transaction);
        lenient().doAnswer(inv -> { inv.getArgument(0, java.util.function.Consumer.class).accept(transaction); return null; })
                .when(transaction).performAndCloseV(any());
        lenient().when(transaction.getDao(ReceiptDao.class)).thenReturn(receiptDao);
    }

    // --- createPaymentIntent ---

    @Test
    void createPaymentIntent_mapsResponseCorrectly() throws StripeException {
        final var pi = mock(PaymentIntent.class);
        when(pi.getId()).thenReturn("pi_test_001");
        when(pi.getClientSecret()).thenReturn("pi_test_001_secret");
        when(gateway.createPaymentIntent(any(), isNull())).thenReturn(pi);

        final var response = service.createPaymentIntent(CreatePaymentIntentRequest.of(1500L, "usd", "cus_test"));

        assertEquals("pi_test_001", response.paymentIntentId());
        assertEquals("pi_test_001_secret", response.clientSecret());
    }

    @Test
    void createPaymentIntent_passesParamsToGateway() throws StripeException {
        when(gateway.createPaymentIntent(any(), isNull())).thenReturn(mock(PaymentIntent.class));

        service.createPaymentIntent(CreatePaymentIntentRequest.of(2000L, "eur", "cus_eu"));

        verify(gateway).createPaymentIntent(argThat(p ->
                p.getAmount() == 2000L && "eur".equals(p.getCurrency()) && "cus_eu".equals(p.getCustomer())
        ), isNull());
    }

    @Test
    void createPaymentIntent_withIdempotencyKey_passesItToGateway() throws StripeException {
        when(gateway.createPaymentIntent(any(), eq("idem-key"))).thenReturn(mock(PaymentIntent.class));

        final var request = new CreatePaymentIntentRequest(1000L, "usd", "cus_test",
                null, null, null, null, "idem-key");
        service.createPaymentIntent(request);

        verify(gateway).createPaymentIntent(any(), eq("idem-key"));
    }

    @Test
    void createPaymentIntent_stripeException_wrapsAsInternalServerError() throws StripeException {
        when(gateway.createPaymentIntent(any(), any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.createPaymentIntent(CreatePaymentIntentRequest.of(1000L, "usd", null)));
    }

    @Test
    void createPaymentIntent_withDescription_passesItToGateway() throws StripeException {
        when(gateway.createPaymentIntent(any(), any())).thenReturn(mock(PaymentIntent.class));

        final var request = new CreatePaymentIntentRequest(1000L, "usd", "cus_test",
                "Gold add-on purchase", null, null, null, null);
        service.createPaymentIntent(request);

        verify(gateway).createPaymentIntent(argThat(p ->
                "Gold add-on purchase".equals(p.getDescription())
        ), isNull());
    }

    @Test
    void createPaymentIntent_withMetadata_mergesIntoParams() throws StripeException {
        when(gateway.createPaymentIntent(any(), any())).thenReturn(mock(PaymentIntent.class));

        final var request = new CreatePaymentIntentRequest(1000L, "usd", "cus_test",
                null, Map.of("addon_id", "addon_001"), null, null, null);
        service.createPaymentIntent(request);

        verify(gateway).createPaymentIntent(argThat(p ->
                "addon_001".equals(p.getMetadata().get("addon_id"))
        ), isNull());
    }

    @Test
    void createPaymentIntent_includesUserIdInMetadata() throws StripeException {
        when(gateway.createPaymentIntent(any(), any())).thenReturn(mock(PaymentIntent.class));

        service.createPaymentIntent(CreatePaymentIntentRequest.of(1000L, "usd", null));

        verify(gateway).createPaymentIntent(argThat(p ->
                "user_test_001".equals(p.getMetadata().get(StripeService.METADATA_USER_ID))
        ), isNull());
    }

    // --- getSubscriptionStatus ---

    @Test
    void getSubscriptionStatus_mapsResponseCorrectly() throws StripeException {
        final var sub = mock(Subscription.class);
        when(sub.getId()).thenReturn("sub_test_001");
        when(sub.getStatus()).thenReturn("active");
        when(sub.getCurrentPeriodEnd()).thenReturn(1893456000L);
        when(gateway.retrieveSubscription("sub_test_001")).thenReturn(sub);

        final var response = service.getSubscriptionStatus("sub_test_001");

        assertEquals("sub_test_001", response.subscriptionId());
        assertEquals("active", response.status());
        assertTrue(response.currentPeriodEnd().startsWith("2030-"));
    }

    @Test
    void getSubscriptionStatus_nullPeriodEnd_returnedAsNull() throws StripeException {
        final var sub = mock(Subscription.class);
        when(sub.getId()).thenReturn("sub_test_002");
        when(sub.getStatus()).thenReturn("canceled");
        when(sub.getCurrentPeriodEnd()).thenReturn(null);
        when(gateway.retrieveSubscription("sub_test_002")).thenReturn(sub);

        assertNull(service.getSubscriptionStatus("sub_test_002").currentPeriodEnd());
    }

    @Test
    void getSubscriptionStatus_stripeException_wrapsAsInternalServerError() throws StripeException {
        when(gateway.retrieveSubscription(any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.getSubscriptionStatus("sub_missing"));
    }

    // --- recordPaymentReceipt ---

    @Test
    void recordPaymentReceipt_withUserId_savesReceiptToDao() {
        final var lookedUpUser = mock(User.class);
        when(userService.getUser("user_001")).thenReturn(lookedUpUser);

        service.recordPaymentReceipt("pi_receipt_001", 3000L, "gbp", "user_001");

        final var captor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptDao).createReceipt(captor.capture());
        final var receipt = captor.getValue();
        assertEquals("pi_receipt_001", receipt.getOriginalTransactionId());
        assertEquals("stripe", receipt.getSchema());
        assertEquals(lookedUpUser, receipt.getUser());
        assertTrue(receipt.getBody().contains("3000"));
        assertTrue(receipt.getBody().contains("gbp"));
    }

    @Test
    void recordPaymentReceipt_nullUserId_skipsDao() {
        service.recordPaymentReceipt("pi_skip_001", 1000L, "usd", null);
        verifyNoInteractions(receiptDao);
        verify(userService, never()).getUser(any());
    }

    @Test
    void recordPaymentReceipt_blankUserId_skipsDao() {
        service.recordPaymentReceipt("pi_skip_002", 1000L, "usd", "  ");
        verifyNoInteractions(receiptDao);
        verify(userService, never()).getUser(any());
    }

    // --- createBillingPortalSession ---

    @Test
    void createBillingPortalSession_mapsUrlFromSession() throws StripeException {
        final var session = mock(Session.class);
        when(session.getUrl()).thenReturn("https://billing.stripe.com/session/abc");
        when(gateway.createBillingPortalSession(any())).thenReturn(session);

        assertEquals("https://billing.stripe.com/session/abc",
                service.createBillingPortalSession("cus_test", null));
    }

    @Test
    void createBillingPortalSession_withReturnUrl_passesItToGateway() throws StripeException {
        final var session = mock(Session.class);
        when(gateway.createBillingPortalSession(any())).thenReturn(session);

        service.createBillingPortalSession("cus_abc", "https://example.com/return");

        verify(gateway).createBillingPortalSession(argThat(p ->
                "https://example.com/return".equals(p.getReturnUrl())
        ));
    }

    @Test
    void createBillingPortalSession_nullReturnUrl_omitsReturnUrl() throws StripeException {
        final var session = mock(Session.class);
        when(gateway.createBillingPortalSession(any())).thenReturn(session);

        service.createBillingPortalSession("cus_abc", null);

        final var captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(gateway).createBillingPortalSession(captor.capture());
        assertNull(captor.getValue().getReturnUrl());
    }

    // --- listSubscriptionsByCustomer ---

    @Test
    void listSubscriptionsByCustomer_mapsResultsAndCursor() throws StripeException {
        final var sub = mock(Subscription.class);
        when(sub.getId()).thenReturn("sub_001");
        when(sub.getStatus()).thenReturn("active");
        when(sub.getCurrentPeriodEnd()).thenReturn(1893456000L);
        when(subscriptionCollection.getData()).thenReturn(List.of(sub));
        when(subscriptionCollection.getHasMore()).thenReturn(false);
        when(gateway.listSubscriptions(any())).thenReturn(subscriptionCollection);

        final var result = service.listSubscriptionsByCustomer("cus_test", "active", 10, null);

        assertEquals("sub_001", result.subscriptions().getFirst().subscriptionId());
        assertEquals("active", result.subscriptions().getFirst().status());
        assertFalse(result.hasMore());
        assertEquals("sub_001", result.nextCursor());
    }

    @Test
    void listSubscriptionsByCustomer_passesStatusToGateway() throws StripeException {
        when(subscriptionCollection.getData()).thenReturn(List.of());
        when(subscriptionCollection.getHasMore()).thenReturn(false);
        when(gateway.listSubscriptions(any())).thenReturn(subscriptionCollection);

        service.listSubscriptionsByCustomer("cus_test", "past_due", 10, null);

        final var captor = ArgumentCaptor.forClass(SubscriptionListParams.class);
        verify(gateway).listSubscriptions(captor.capture());
        assertEquals(SubscriptionListParams.Status.PAST_DUE, captor.getValue().getStatus());
    }

    @Test
    void listSubscriptionsByCustomer_stripeException_wrapsAsInternalServerError() throws StripeException {
        when(gateway.listSubscriptions(any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.listSubscriptionsByCustomer("cus_test", null, 10, null));
    }

    // --- recordMeterEvent ---

    @Test
    void recordMeterEvent_passesAllFieldsToGateway() throws StripeException {
        service.recordMeterEvent("cus_test", "api_requests", 25, "idem-key-abc");

        final var captor = ArgumentCaptor.forClass(com.stripe.param.billing.MeterEventCreateParams.class);
        verify(gateway).createMeterEvent(captor.capture(), eq("idem-key-abc"));

        final var params = captor.getValue();
        assertEquals("api_requests", params.getEventName());
        assertEquals("idem-key-abc", params.getIdentifier());
        assertEquals("cus_test", params.getPayload().get("stripe_customer_id"));
        assertEquals("25", params.getPayload().get("value"));
    }

    // --- createSubscription ---

    @Test
    void createSubscription_mapsSubscriptionResponse() throws StripeException {
        final var sub = mock(Subscription.class);
        when(sub.getId()).thenReturn("sub_new");
        when(sub.getStatus()).thenReturn("active");
        when(sub.getCurrentPeriodEnd()).thenReturn(1893456000L);
        when(gateway.createSubscription(any(), isNull())).thenReturn(sub);

        final var result = service.createSubscription("cus_test", CreateSubscriptionRequest.of("price_test"));

        assertEquals("sub_new", result.subscriptionId());
        assertEquals("active", result.status());
        assertNotNull(result.currentPeriodEnd());
    }

    @Test
    void createSubscription_passesCustomerAndPriceToGateway() throws StripeException {
        final var sub = mock(Subscription.class);
        when(sub.getCurrentPeriodEnd()).thenReturn(null);
        when(gateway.createSubscription(any(), isNull())).thenReturn(sub);

        service.createSubscription("cus_abc", CreateSubscriptionRequest.of("price_xyz"));

        final var captor = ArgumentCaptor.forClass(SubscriptionCreateParams.class);
        verify(gateway).createSubscription(captor.capture(), isNull());
        assertEquals("cus_abc", captor.getValue().getCustomer());
        assertEquals("price_xyz", captor.getValue().getItems().getFirst().getPrice());
    }

    @Test
    void createSubscription_withMetadataAndIdempotencyKey_passesThrough() throws StripeException {
        final var sub = mock(Subscription.class);
        when(sub.getCurrentPeriodEnd()).thenReturn(null);
        when(gateway.createSubscription(any(), eq("sub-idem-key"))).thenReturn(sub);

        final var request = new CreateSubscriptionRequest("price_xyz", "Gold plan", Map.of("org_id", "org_001"), "sub-idem-key");
        service.createSubscription("cus_abc", request);

        verify(gateway).createSubscription(argThat(p -> {
                @SuppressWarnings("unchecked")
                var meta = (java.util.Map<String, String>) p.getMetadata();
                return "Gold plan".equals(p.getDescription()) && "org_001".equals(meta.get("org_id"));
        }), eq("sub-idem-key"));
    }

    @Test
    void createSubscription_stripeException_wrapsAppropriately() throws StripeException {
        when(gateway.createSubscription(any(), any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.createSubscription("cus_test", CreateSubscriptionRequest.of("price_test")));
    }

    // --- cancelSubscription ---

    @Test
    void cancelSubscription_returnsCanceledStatus() throws StripeException {
        final var sub = mock(Subscription.class);
        when(sub.getId()).thenReturn("sub_cancel");
        when(sub.getStatus()).thenReturn("canceled");
        when(sub.getCurrentPeriodEnd()).thenReturn(null);
        when(gateway.cancelSubscription("sub_cancel")).thenReturn(sub);

        final var result = service.cancelSubscription("sub_cancel");

        assertEquals("sub_cancel", result.subscriptionId());
        assertEquals("canceled", result.status());
        assertNull(result.currentPeriodEnd());
    }

    @Test
    void cancelSubscription_stripeException_wrapsAppropriately() throws StripeException {
        when(gateway.cancelSubscription(any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.cancelSubscription("sub_test"));
    }

    // --- listProducts ---

    @Test
    void listProducts_mapsResponseCorrectly() throws StripeException {
        final var price = mock(Price.class);
        when(price.getId()).thenReturn("price_001");
        when(price.getProduct()).thenReturn("prod_001");
        when(price.getCurrency()).thenReturn("usd");
        when(price.getType()).thenReturn("recurring");
        when(price.getUnitAmount()).thenReturn(999L);
        final var recurring = mock(com.stripe.model.Price.Recurring.class);
        when(recurring.getInterval()).thenReturn("month");
        when(price.getRecurring()).thenReturn(recurring);

        final var product = mock(com.stripe.model.Product.class);
        when(product.getId()).thenReturn("prod_001");
        when(product.getName()).thenReturn("Gold Plan");
        when(product.getDescription()).thenReturn("Our best plan");
        when(product.getActive()).thenReturn(true);
        when(product.getDefaultPriceObject()).thenReturn(price);
        when(productCollection.getData()).thenReturn(List.of(product));
        when(gateway.listProducts(any())).thenReturn(productCollection);

        final var result = service.listProducts(true, 10);

        assertEquals(1, result.size());
        final var summary = result.getFirst();
        assertEquals("prod_001", summary.id());
        assertEquals("Gold Plan", summary.name());
        assertEquals("Our best plan", summary.description());
        assertTrue(summary.active());
        assertNotNull(summary.defaultPrice());
        assertEquals("price_001", summary.defaultPrice().id());
        assertEquals(999L, summary.defaultPrice().unitAmount());
        assertEquals("usd", summary.defaultPrice().currency());
        assertEquals("month", summary.defaultPrice().interval());
    }

    @Test
    void listProducts_nullDefaultPrice_mappedAsNull() throws StripeException {
        final var product = mock(com.stripe.model.Product.class);
        when(product.getId()).thenReturn("prod_002");
        when(product.getName()).thenReturn("Free Tier");
        when(product.getActive()).thenReturn(true);
        when(product.getDefaultPriceObject()).thenReturn(null);
        when(productCollection.getData()).thenReturn(List.of(product));
        when(gateway.listProducts(any())).thenReturn(productCollection);

        final var result = service.listProducts(true, 10);

        assertNull(result.getFirst().defaultPrice());
    }

    @Test
    void listProducts_passesFiltersAndExpandToGateway() throws StripeException {
        when(productCollection.getData()).thenReturn(List.of());
        when(gateway.listProducts(any())).thenReturn(productCollection);

        service.listProducts(false, 50);

        final var captor = ArgumentCaptor.forClass(com.stripe.param.ProductListParams.class);
        verify(gateway).listProducts(captor.capture());
        final var params = captor.getValue();
        assertEquals(Boolean.FALSE, params.getActive());
        assertEquals(50L, params.getLimit());
        assertTrue(params.getExpand().contains("data.default_price"));
    }

    // --- listPrices ---

    @Test
    void listPrices_cacheMiss_fetchesFromStripeAndCaches() throws StripeException {
        when(priceCache.get(any())).thenReturn(null);

        final var price = mock(Price.class);
        when(price.getId()).thenReturn("price_001");
        when(price.getProduct()).thenReturn("prod_001");
        when(price.getCurrency()).thenReturn("usd");
        when(price.getType()).thenReturn("recurring");
        when(price.getUnitAmount()).thenReturn(999L);
        final var recurring = mock(com.stripe.model.Price.Recurring.class);
        when(recurring.getInterval()).thenReturn("month");
        when(price.getRecurring()).thenReturn(recurring);
        when(priceCollection.getData()).thenReturn(List.of(price));
        when(gateway.listPrices(any())).thenReturn(priceCollection);

        final var result = service.listPrices("prod_001", true, 10);

        assertEquals(1, result.size());
        assertEquals("month", result.getFirst().interval());
        verify(priceCache).put(any(), eq(result));
    }

    @Test
    void listPrices_cacheHit_doesNotCallGateway() throws StripeException {
        final var cached = List.of(new PriceSummary("price_001", "prod_001", null, 999L, "usd", "recurring", "month"));
        when(priceCache.get(any())).thenReturn(cached);

        assertSame(cached, service.listPrices("prod_001", true, 10));
        verifyNoInteractions(gateway);
    }

    @Test
    void listPrices_passesFiltersToGateway() throws StripeException {
        when(priceCache.get(any())).thenReturn(null);
        when(priceCollection.getData()).thenReturn(List.of());
        when(gateway.listPrices(any())).thenReturn(priceCollection);

        service.listPrices("prod_filter", false, 25);

        final var captor = ArgumentCaptor.forClass(PriceListParams.class);
        verify(gateway).listPrices(captor.capture());
        assertEquals("prod_filter", captor.getValue().getProduct());
        assertEquals(Boolean.FALSE, captor.getValue().getActive());
        assertEquals(25L, captor.getValue().getLimit());
    }

    // --- retrievePrice ---

    @Test
    void retrievePrice_mapsResponseCorrectly() throws StripeException {
        final var price = mock(Price.class);
        when(price.getId()).thenReturn("price_single");
        when(price.getProduct()).thenReturn("prod_001");
        when(price.getNickname()).thenReturn("Gold Monthly");
        when(price.getUnitAmount()).thenReturn(999L);
        when(price.getCurrency()).thenReturn("usd");
        when(price.getType()).thenReturn("recurring");
        final var recurring = mock(com.stripe.model.Price.Recurring.class);
        when(recurring.getInterval()).thenReturn("month");
        when(price.getRecurring()).thenReturn(recurring);
        when(gateway.retrievePrice("price_single")).thenReturn(price);

        final var result = service.retrievePrice("price_single");

        assertEquals("price_single", result.id());
        assertEquals("Gold Monthly", result.nickname());
        assertEquals("month", result.interval());
    }

    // --- findCustomerByMetadata ---

    @Test
    void findCustomerByMetadata_found_returnsCustomerId() throws StripeException {
        final var customer = mock(Customer.class);
        when(customer.getId()).thenReturn("cus_found");
        final var searchResult = mock(CustomerSearchResult.class);
        when(searchResult.getData()).thenReturn(List.of(customer));
        when(gateway.searchCustomers(any())).thenReturn(searchResult);

        assertTrue(service.findCustomerByMetadata("orgId", "org_001").isPresent());
        assertEquals("cus_found", service.findCustomerByMetadata("orgId", "org_001").get());
    }

    @Test
    void findCustomerByMetadata_notFound_returnsEmpty() throws StripeException {
        final var searchResult = mock(CustomerSearchResult.class);
        when(searchResult.getData()).thenReturn(List.of());
        when(gateway.searchCustomers(any())).thenReturn(searchResult);

        assertTrue(service.findCustomerByMetadata("orgId", "org_missing").isEmpty());
    }

    // --- createCheckoutSession ---

    @Test
    void createCheckoutSession_mapsResponse() throws StripeException {
        final var session = mock(com.stripe.model.checkout.Session.class);
        when(session.getId()).thenReturn("cs_test_001");
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_001");
        when(gateway.createCheckoutSession(any(), isNull())).thenReturn(session);

        final var request = new CreateCheckoutSessionRequest(
                "cus_test", "price_001", "https://example.com/success", "https://example.com/cancel", null, null, null);
        final var result = service.createCheckoutSession(request);

        assertEquals("cs_test_001", result.sessionId());
    }

    @Test
    void createCheckoutSession_withIdempotencyKey_passesItToGateway() throws StripeException {
        final var session = mock(com.stripe.model.checkout.Session.class);
        when(gateway.createCheckoutSession(any(), eq("checkout-idem"))).thenReturn(session);

        final var request = new CreateCheckoutSessionRequest(
                "cus_test", "price_001", "https://example.com/success", "https://example.com/cancel", null, "checkout-idem", null);
        service.createCheckoutSession(request);

        verify(gateway).createCheckoutSession(any(), eq("checkout-idem"));
    }

    @Test
    void createCheckoutSession_nullMode_defaultsToSubscription() throws StripeException {
        when(gateway.createCheckoutSession(any(), any())).thenReturn(mock(com.stripe.model.checkout.Session.class));

        final var request = new CreateCheckoutSessionRequest(
                "cus_test", "price_001", "https://example.com/success", "https://example.com/cancel", null, null, null);
        service.createCheckoutSession(request);

        verify(gateway).createCheckoutSession(argThat(p ->
                p.getMode() == com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION
        ), isNull());
    }

    @Test
    void createCheckoutSession_withMetadata_propagatesToSessionAndSubscriptionData() throws StripeException {
        when(gateway.createCheckoutSession(any(), any())).thenReturn(mock(com.stripe.model.checkout.Session.class));

        final var meta = Map.of("orgId", "org_001", "addonId", "addon_xyz");
        final var request = new CreateCheckoutSessionRequest(
                "cus_test", "price_001", "https://example.com/success", "https://example.com/cancel",
                "subscription", null, meta);
        service.createCheckoutSession(request);

        verify(gateway).createCheckoutSession(argThat(p -> {
            final var sessionMeta = p.getMetadata();
            final var subData = p.getSubscriptionData();
            if (sessionMeta == null || subData == null) return false;
            final var subMeta = subData.getMetadata();
            return sessionMeta.containsKey("orgId") && subMeta != null && subMeta.containsKey("orgId");
        }), isNull());
    }

    @Test
    void createCheckoutSession_withMetadataAndPaymentMode_propagatesToPaymentIntentData() throws StripeException {
        when(gateway.createCheckoutSession(any(), any())).thenReturn(mock(com.stripe.model.checkout.Session.class));

        final var meta = Map.of("orgId", "org_002");
        final var request = new CreateCheckoutSessionRequest(
                "cus_test", "price_001", "https://example.com/success", "https://example.com/cancel",
                "payment", null, meta);
        service.createCheckoutSession(request);

        verify(gateway).createCheckoutSession(argThat(p -> {
            final var piData = p.getPaymentIntentData();
            if (piData == null) return false;
            final var piMeta = piData.getMetadata();
            return piMeta != null && piMeta.containsKey("orgId");
        }), isNull());
    }

    // --- listInvoices ---

    @Test
    void listInvoices_mapsResponseCorrectly() throws StripeException {
        final var invoice = mock(Invoice.class);
        when(invoice.getId()).thenReturn("in_001");
        when(invoice.getSubscription()).thenReturn("sub_001");
        when(invoice.getAmountPaid()).thenReturn(999L);
        when(invoice.getCurrency()).thenReturn("usd");
        when(invoice.getStatus()).thenReturn("paid");
        when(invoice.getCreated()).thenReturn(1893456000L);
        when(invoiceCollection.getData()).thenReturn(List.of(invoice));
        when(gateway.listInvoices(any())).thenReturn(invoiceCollection);

        final var result = service.listInvoices("cus_test", 10, null);

        assertEquals(1, result.size());
        final var summary = result.getFirst();
        assertEquals("in_001", summary.id());
        assertEquals("sub_001", summary.subscriptionId());
        assertEquals(999L, summary.amountPaid());
        assertEquals("usd", summary.currency());
        assertEquals("paid", summary.status());
        assertNotNull(summary.createdAt());
    }

    @Test
    void listInvoices_passesLimitAndCursorToGateway() throws StripeException {
        when(invoiceCollection.getData()).thenReturn(List.of());
        when(gateway.listInvoices(any())).thenReturn(invoiceCollection);

        service.listInvoices("cus_test", 20, "in_cursor");

        verify(gateway).listInvoices(argThat(p ->
                "cus_test".equals(p.getCustomer())
                && p.getLimit() == 20L
                && "in_cursor".equals(p.getStartingAfter())
        ));
    }

    @Test
    void listInvoices_stripeException_wrapsAppropriately() throws StripeException {
        when(gateway.listInvoices(any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.listInvoices("cus_test", 10, null));
    }

    // --- updateCustomer ---

    @Test
    void updateCustomer_callsGatewayWithParams() throws StripeException {
        when(gateway.updateCustomer(eq("cus_test"), any())).thenReturn(mock(Customer.class));

        service.updateCustomer("cus_test", "new@example.com", "New Name");

        verify(gateway).updateCustomer(eq("cus_test"), argThat(p ->
                "new@example.com".equals(p.getEmail()) && "New Name".equals(p.getName())
        ));
    }

    @Test
    void updateCustomer_nullEmail_omitsEmailFromParams() throws StripeException {
        when(gateway.updateCustomer(any(), any())).thenReturn(mock(Customer.class));

        service.updateCustomer("cus_test", null, "New Name");

        verify(gateway).updateCustomer(any(), argThat(p -> p.getEmail() == null));
    }

    @Test
    void updateCustomer_stripeException_wrapsAppropriately() throws StripeException {
        when(gateway.updateCustomer(any(), any())).thenThrow(mock(StripeException.class));

        assertThrows(InternalServerErrorException.class,
                () -> service.updateCustomer("cus_test", "x@x.com", "X"));
    }

}
