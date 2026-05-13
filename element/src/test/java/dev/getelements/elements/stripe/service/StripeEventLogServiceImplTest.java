package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.dao.StripeEventLogDao;
import dev.getelements.elements.stripe.model.StripeEventLogDocument;
import dev.getelements.elements.stripe.model.StripeEventLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeEventLogServiceImplTest {

    @Mock
    private StripeEventLogDao eventLogDao;

    private StripeEventLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StripeEventLogServiceImpl(eventLogDao);
    }

    // --- logEvent ---

    @Test
    void logEvent_savesDocumentWithCorrectFields() {

        service.logEvent("evt_001", "payment_intent.succeeded");

        final var captor = ArgumentCaptor.forClass(StripeEventLogDocument.class);
        verify(eventLogDao).save(captor.capture());

        final var doc = captor.getValue();
        assertEquals("evt_001", doc.getStripeEventId());
        assertEquals("payment_intent.succeeded", doc.getEventType());
        assertTrue(doc.getReceivedAt() > 0);
    }

    @Test
    void logEvent_setsReceivedAtToCurrentTime() {

        final long before = System.currentTimeMillis();
        service.logEvent("evt_002", "invoice.payment_succeeded");
        final long after = System.currentTimeMillis();

        final var captor = ArgumentCaptor.forClass(StripeEventLogDocument.class);
        verify(eventLogDao).save(captor.capture());

        final long receivedAt = captor.getValue().getReceivedAt();
        assertTrue(receivedAt >= before && receivedAt <= after,
                "receivedAt should be between " + before + " and " + after + " but was " + receivedAt);
    }

    // --- listEvents ---

    @Test
    void listEvents_mapsDocumentsToEntries() {

        final var doc = new StripeEventLogDocument();
        doc.setStripeEventId("evt_003");
        doc.setEventType("customer.subscription.created");
        doc.setReceivedAt(1893456000000L);

        when(eventLogDao.findEvents(null, 20, 0)).thenReturn(List.of(doc));
        when(eventLogDao.countEvents(null)).thenReturn(1L);

        final var result = service.listEvents(null, 20, 0);

        assertEquals(1, result.events().size());
        final StripeEventLogEntry entry = result.events().getFirst();
        assertEquals("evt_003", entry.stripeEventId());
        assertEquals("customer.subscription.created", entry.eventType());
        assertNotNull(entry.receivedAt());
        assertTrue(entry.receivedAt().startsWith("2030-"));
    }

    @Test
    void listEvents_hasMore_trueWhenMoreResultsExist() {

        when(eventLogDao.findEvents(any(), eq(10), eq(0))).thenReturn(List.of(
                makeDoc("evt_a", "payment_intent.succeeded"),
                makeDoc("evt_b", "payment_intent.succeeded")
        ));
        when(eventLogDao.countEvents(any())).thenReturn(5L);

        final var result = service.listEvents(null, 10, 0);

        assertTrue(result.hasMore());
        assertEquals(5L, result.total());
    }

    @Test
    void listEvents_hasMore_falseWhenAllResultsReturned() {

        when(eventLogDao.findEvents(any(), eq(10), eq(0))).thenReturn(List.of(
                makeDoc("evt_a", "invoice.payment_succeeded")
        ));
        when(eventLogDao.countEvents(any())).thenReturn(1L);

        final var result = service.listEvents(null, 10, 0);

        assertFalse(result.hasMore());
    }

    @Test
    void listEvents_passesTypeFilterToDao() {

        when(eventLogDao.findEvents("payment_intent.succeeded", 20, 0)).thenReturn(List.of());
        when(eventLogDao.countEvents("payment_intent.succeeded")).thenReturn(0L);

        service.listEvents("payment_intent.succeeded", 20, 0);

        verify(eventLogDao).findEvents("payment_intent.succeeded", 20, 0);
        verify(eventLogDao).countEvents("payment_intent.succeeded");
    }

    @Test
    void listEvents_passesOffsetToDao() {

        when(eventLogDao.findEvents(null, 20, 40)).thenReturn(List.of());
        when(eventLogDao.countEvents(null)).thenReturn(100L);

        service.listEvents(null, 20, 40);

        verify(eventLogDao).findEvents(null, 20, 40);
    }

    // --- helpers ---

    private static StripeEventLogDocument makeDoc(String eventId, String eventType) {
        final var doc = new StripeEventLogDocument();
        doc.setStripeEventId(eventId);
        doc.setEventType(eventType);
        doc.setReceivedAt(System.currentTimeMillis());
        return doc;
    }

}
