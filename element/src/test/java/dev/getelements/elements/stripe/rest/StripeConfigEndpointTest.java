package dev.getelements.elements.stripe.rest;

import dev.getelements.elements.stripe.model.StripeConfig;
import dev.getelements.elements.stripe.model.StripeDualConfig;
import dev.getelements.elements.stripe.model.StripeMode;
import dev.getelements.elements.stripe.service.StripeConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The endpoint no longer masks server-side: GET returns plaintext and UI masking is client-side
 * (password fields with optional reveal). PUT persists values verbatim, rejecting any submission
 * still containing the UI mask glyph — a stale pre-migration client could otherwise persist a
 * masked placeholder literal and shadow the real credential (issue #12).
 */
@ExtendWith(MockitoExtension.class)
class StripeConfigEndpointTest {

    @Mock private StripeConfigService configService;

    private StripeConfigEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new StripeConfigEndpoint(configService);
    }

    // --- GET (plaintext) ---

    @Test
    void getConfig_returnsEffectiveConfigIncludingAttributeFallback() {
        when(configService.getConfig(StripeMode.PRODUCTION)).thenReturn(new StripeConfig("sk_live_prodKeyAb", "whsec_prodSecret12"));
        when(configService.getConfig(StripeMode.SANDBOX)).thenReturn(new StripeConfig("sk_test_testKeyCd", "whsec_testSecret34"));

        final var result = endpoint.getConfig();

        assertEquals(new StripeConfig("sk_live_prodKeyAb", "whsec_prodSecret12"), result.production());
        assertEquals(new StripeConfig("sk_test_testKeyCd", "whsec_testSecret34"), result.sandbox());
    }

    // --- PUT (verbatim persistence) ---

    @Test
    void saveConfig_persistsProductionAndSandboxVerbatim() {
        final var submitted = new StripeDualConfig(
                new StripeConfig("sk_live_new", "whsec_new"),
                new StripeConfig("sk_test_new", "whsec_test_new"));

        endpoint.saveConfig(submitted);

        verify(configService).saveConfig(new StripeConfig("sk_live_new", "whsec_new"), StripeMode.PRODUCTION);
        verify(configService).saveConfig(new StripeConfig("sk_test_new", "whsec_test_new"), StripeMode.SANDBOX);
    }

    @Test
    void saveConfig_emptyString_clearsField() {
        final var submitted = new StripeDualConfig(new StripeConfig("", "whsec_livesecret1234"), null);

        endpoint.saveConfig(submitted);

        verify(configService).saveConfig(new StripeConfig("", "whsec_livesecret1234"), StripeMode.PRODUCTION);
        verify(configService, never()).saveConfig(any(), eq(StripeMode.SANDBOX));
    }

    // --- PUT (mask-shape rejection) ---

    @Test
    void saveConfig_maskGlyphInApiKey_isRejectedWithoutSaving() {
        final var submitted = new StripeDualConfig(new StripeConfig("••••BLQF", "real_secret"), null);

        assertThrows(jakarta.ws.rs.BadRequestException.class, () -> endpoint.saveConfig(submitted));
        verify(configService, never()).saveConfig(any(), any());
    }

    @Test
    void saveConfig_maskGlyphInWebhookSecret_isRejectedWithoutSaving() {
        final var submitted = new StripeDualConfig(new StripeConfig("real_key", "••••5aDK"), null);

        assertThrows(jakarta.ws.rs.BadRequestException.class, () -> endpoint.saveConfig(submitted));
        verify(configService, never()).saveConfig(any(), any());
    }

    @Test
    void saveConfig_maskGlyphInSandbox_isRejectedWithoutSavingProduction() {
        final var submitted = new StripeDualConfig(
                new StripeConfig("real_key", "real_secret"),
                new StripeConfig("••••BLQF", ""));

        assertThrows(jakarta.ws.rs.BadRequestException.class, () -> endpoint.saveConfig(submitted));
        // Production was validated first and must not have been persisted either; the whole
        // submission fails atomically.
        verify(configService, never()).saveConfig(any(), any());
    }

    // --- edge cases ---

    @Test
    void saveConfig_nullModeSections_areIgnored() {
        final var submitted = new StripeDualConfig(null, null);

        endpoint.saveConfig(submitted);

        verify(configService, never()).saveConfig(any(), any());
    }

}
