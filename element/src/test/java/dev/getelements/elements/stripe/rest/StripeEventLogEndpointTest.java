package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.StripeEventLogEntry;
import dev.getelements.elements.stripe.model.StripeEventLogResponse;
import dev.getelements.elements.stripe.service.StripeEventLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeEventLogEndpointTest {

    @Mock
    private StripeEventLogService eventLogService;

    private StripeEventLogEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripeEventLogEndpoint(eventLogService);
    }

    @Test
    void listEvents_delegatesToServiceWithDefaultParams() {

        final var expected = new StripeEventLogResponse(List.of(), 0L, false);
        when(eventLogService.listEvents(null, 20, 0)).thenReturn(expected);

        assertSame(expected, endpoint.listEvents(null, 20, 0));
        verify(eventLogService).listEvents(null, 20, 0);
    }

    @Test
    void listEvents_withTypeFilter_passesTypeToService() {

        final var entry = new StripeEventLogEntry("evt_001", "payment_intent.succeeded", "2030-01-01T00:00:00Z");
        final var expected = new StripeEventLogResponse(List.of(entry), 1L, false);
        when(eventLogService.listEvents("payment_intent.succeeded", 10, 0)).thenReturn(expected);

        assertSame(expected, endpoint.listEvents("payment_intent.succeeded", 10, 0));
        verify(eventLogService).listEvents("payment_intent.succeeded", 10, 0);
    }

    @Test
    void listEvents_withOffsetPagination_passesOffsetToService() {

        final var expected = new StripeEventLogResponse(List.of(), 50L, false);
        when(eventLogService.listEvents(null, 20, 40)).thenReturn(expected);

        assertSame(expected, endpoint.listEvents(null, 20, 40));
        verify(eventLogService).listEvents(null, 20, 40);
    }

}
