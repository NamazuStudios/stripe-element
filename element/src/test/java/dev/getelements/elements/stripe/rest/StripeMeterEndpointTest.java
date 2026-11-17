package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.MeterSummary;
import dev.getelements.elements.stripe.model.MeterUsageSummary;
import dev.getelements.elements.stripe.service.StripeService;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeMeterEndpointTest {

    @Mock
    private StripeService stripeService;

    private StripeMeterEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripeMeterEndpoint(stripeService);
    }

    @Test
    void listMeters_delegatesToService() {
        final var expected = List.of(new MeterSummary("mtr_001", "API Requests", "api_requests", "active", null));
        when(stripeService.listMeters(true, 100)).thenReturn(expected);

        assertSame(expected, endpoint.listMeters(true, 100, null));
        verify(stripeService).listMeters(true, 100);
    }

    @Test
    void listMeterUsage_delegatesToService() {

        final var expected = List.of(new MeterUsageSummary(
                "mtrusg_001", "mtr_001", "api_requests", BigDecimal.TEN,
                "2026-11-15T00:00:00Z", "2026-11-15T01:00:00Z"));
        when(stripeService.listMeterUsage("api_requests", "cus_test", null, null, null)).thenReturn(expected);

        assertSame(expected, endpoint.listMeterUsage("api_requests", "cus_test", null, null, null, null));
        verify(stripeService).listMeterUsage("api_requests", "cus_test", null, null, null);
    }

    @Test
    void listMeterUsage_parsesIsoTimestamps() {

        endpoint.listMeterUsage("api_requests", "cus_test", "2026-11-15T00:00:00Z", "2026-11-15T12:00:00Z", "hour", null);

        verify(stripeService).listMeterUsage(
                eq("api_requests"), eq("cus_test"),
                eq(Instant.parse("2026-11-15T00:00:00Z")),
                eq(Instant.parse("2026-11-15T12:00:00Z")),
                eq("hour"));
    }

    @Test
    void listMeterUsage_malformedTimestamp_rejected() {
        assertThrows(BadRequestException.class, () ->
                endpoint.listMeterUsage("api_requests", "cus_test", "not-a-date", null, null, null));
    }

}
