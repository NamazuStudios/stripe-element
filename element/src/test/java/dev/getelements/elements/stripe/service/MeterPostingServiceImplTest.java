package dev.getelements.elements.stripe.service;

import dev.getelements.elements.stripe.dao.MeterPostingDao;
import dev.getelements.elements.stripe.model.MeterPostingDocument;
import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecord;
import dev.getelements.elements.stripe.model.PostingRecordResponse;
import dev.getelements.elements.stripe.model.StripeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterPostingServiceImplTest {

    @Mock
    private MeterPostingDao postingDao;

    private MeterPostingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MeterPostingServiceImpl(postingDao);
    }

    @Test
    void recordPostingOutcome_stampsCreatedAtWhenAbsent() {
        final var record = new PostingRecord(
                "batch-1:org-1:api_requests", "cus_001", "api_requests",
                BigDecimal.TEN, PostingOutcome.POSTED, StripeMode.PRODUCTION, null, null);

        service.recordPostingOutcome(record);

        final var captor = ArgumentCaptor.forClass(MeterPostingDocument.class);
        verify(postingDao).save(captor.capture());
        final var doc = captor.getValue();
        assertEquals("batch-1:org-1:api_requests", doc.getIdentifier());
        assertEquals("cus_001", doc.getCustomerId());
        assertEquals("api_requests", doc.getEventName());
        assertEquals("10", doc.getQuantity());
        assertEquals("POSTED", doc.getOutcome());
        assertEquals("PRODUCTION", doc.getMode());
        assertNotNull(doc.getCreatedAt());
    }

    @Test
    void recordPostingOutcome_preservesExplicitTimestamp() {
        final var record = new PostingRecord(
                "batch-1:org-1:api_requests", "cus_001", "api_requests",
                BigDecimal.ONE, PostingOutcome.SKIPPED_EXEMPT, StripeMode.SANDBOX, "trial org", 12345L);

        service.recordPostingOutcome(record);

        final var captor = ArgumentCaptor.forClass(MeterPostingDocument.class);
        verify(postingDao).save(captor.capture());
        assertEquals(12345L, captor.getValue().getCreatedAt());
        assertEquals("trial org", captor.getValue().getDetail());
    }

    @Test
    void recordPostingOutcome_requiresIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> service.recordPostingOutcome(new PostingRecord(
                null, "cus_001", "api_requests", BigDecimal.ONE,
                PostingOutcome.POSTED, StripeMode.PRODUCTION, null, null)));
    }

    @Test
    void recordPostingOutcome_requiresOutcome() {
        assertThrows(IllegalArgumentException.class, () -> service.recordPostingOutcome(new PostingRecord(
                "batch-1:org-1:api_requests", "cus_001", "api_requests", BigDecimal.ONE,
                null, StripeMode.PRODUCTION, null, null)));
    }

    @Test
    void listPostings_mapsDocumentsToRecords() {
        final var doc = new MeterPostingDocument();
        doc.setIdentifier("batch-1:org-1:api_requests");
        doc.setCustomerId("cus_001");
        doc.setEventName("api_requests");
        doc.setQuantity("10");
        doc.setOutcome("FAILED");
        doc.setMode("SANDBOX");
        doc.setDetail("Stripe error: boom");
        doc.setCreatedAt(1700000000000L);
        when(postingDao.findPostings(any(), any(), any(), any(), any(), eq(20), eq(0)))
                .thenReturn(List.of(doc));
        when(postingDao.countPostings(any(), any(), any(), any(), any())).thenReturn(1L);

        final var response = service.listPostings(null, null, null, null, null, 20, 0);

        assertEquals(1, response.total());
        assertEquals(1, response.records().size());
        final var record = response.records().getFirst();
        assertEquals("batch-1:org-1:api_requests", record.identifier());
        assertEquals(PostingOutcome.FAILED, record.outcome());
        assertEquals(StripeMode.SANDBOX, record.mode());
        assertEquals(new BigDecimal("10"), record.quantity());
        assertEquals("Stripe error: boom", record.detail());
        assertNotNull(record.at());
    }

    @Test
    void listPostings_toleratesUnknownStoredOutcomeOrMode() {
        final var doc = new MeterPostingDocument();
        doc.setIdentifier("batch-1:org-1:api_requests");
        doc.setOutcome("SOMETHING_NEW");
        doc.setMode("WHO_KNOWS");
        doc.setCreatedAt(1700000000000L);
        when(postingDao.findPostings(any(), any(), any(), any(), any(), eq(20), eq(0)))
                .thenReturn(List.of(doc));
        when(postingDao.countPostings(any(), any(), any(), any(), any())).thenReturn(1L);

        final var response = service.listPostings(null, null, null, null, null, 20, 0);

        assertNull(response.records().getFirst().outcome());
        assertNull(response.records().getFirst().mode());
    }

    @Test
    void listPostings_hasMoreReflectsRemainingPages() {
        when(postingDao.findPostings(any(), any(), any(), any(), any(), eq(20), eq(0)))
                .thenReturn(List.of());
        when(postingDao.countPostings(any(), any(), any(), any(), any())).thenReturn(25L);

        assertTrue(service.listPostings(null, null, null, null, null, 20, 0).hasMore());
    }

}
