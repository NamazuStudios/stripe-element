package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.PostingOutcome;
import dev.getelements.elements.stripe.model.PostingRecordResponse;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.service.MeterPostingService;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePostingEndpointTest {

    @Mock
    private MeterPostingService postingService;

    private StripePostingEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripePostingEndpoint(postingService);
    }

    @Test
    void listPostings_noFilters_passesNulls() {
        final var expected = new PostingRecordResponse(List.of(), 0L, false);
        when(postingService.listPostings(null, null, null, null, null, 20, 0)).thenReturn(expected);

        assertSame(expected, endpoint.listPostings(null, null, null, null, null, 20, 0));
    }

    @Test
    void listPostings_modeBoth_isTreatedAsNoModeFilter() {
        when(postingService.listPostings(null, null, null, null, null, 20, 0))
                .thenReturn(new PostingRecordResponse(List.of(), 0L, false));

        endpoint.listPostings(null, null, null, null, "both", 20, 0);

        verify(postingService).listPostings(null, null, null, null, null, 20, 0);
    }

    @Test
    void listPostings_withFilters_passesThemThrough() {
        endpoint.listPostings("batch-1", "cus_001", "api_requests", "failed", "sandbox", 50, 100);

        verify(postingService).listPostings(
                "batch-1", "cus_001", "api_requests", PostingOutcome.FAILED, StripeMode.SANDBOX, 50, 100);
    }

    @Test
    void listPostings_invalidOutcome_rejected() {
        assertThrows(BadRequestException.class, () ->
                endpoint.listPostings(null, null, null, "not-an-outcome", null, 20, 0));
    }

    @Test
    void listPostings_invalidMode_rejected() {
        assertThrows(BadRequestException.class, () ->
                endpoint.listPostings(null, null, null, null, "staging", 20, 0));
    }

}
