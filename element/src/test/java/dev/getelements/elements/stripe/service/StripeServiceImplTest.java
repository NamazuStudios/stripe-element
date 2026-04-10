package dev.getelements.elements.stripe.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import dev.getelements.elements.sdk.dao.ReceiptDao;
import dev.getelements.elements.sdk.dao.Transaction;
import dev.getelements.elements.sdk.model.receipt.Receipt;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.service.user.UserService;
import dev.getelements.elements.stripe.model.CreatePaymentIntentRequest;
import dev.getelements.elements.stripe.model.SubscriptionStatusResponse;
import jakarta.inject.Provider;
import jakarta.ws.rs.InternalServerErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceImplTest {

    @Mock
    private StripeGateway gateway;

    @Mock
    private UserService userService;

    @Mock
    private Provider<Transaction> transactionProvider;

    @Mock
    private Transaction transaction;

    @Mock
    private ReceiptDao receiptDao;

    private StripeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StripeServiceImpl(gateway, userService, transactionProvider);
        final var user = mock(User.class);
        lenient().when(user.getId()).thenReturn("user_test_001");
        lenient().when(userService.getCurrentUser()).thenReturn(user);
        lenient().when(transactionProvider.get()).thenReturn(transaction);
        // By default, performAndCloseV invokes its Consumer so DAO calls are exercised.
        lenient().doAnswer(inv -> { inv.getArgument(0, java.util.function.Consumer.class).accept(transaction); return null; })
                .when(transaction).performAndCloseV(any());
        lenient().when(transaction.getDao(ReceiptDao.class)).thenReturn(receiptDao);
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

    // --- createPaymentIntent metadata ---

    @Test
    void createPaymentIntent_includesUserIdInMetadata() throws StripeException {

        when(gateway.createPaymentIntent(any())).thenReturn(mock(PaymentIntent.class));

        service.createPaymentIntent(new CreatePaymentIntentRequest(1000L, "usd", null));

        verify(gateway).createPaymentIntent(argThat(p ->
                "user_test_001".equals(p.getMetadata().get(StripeService.METADATA_USER_ID))
        ));
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
        assertNotNull(receipt.getBody());
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

}
